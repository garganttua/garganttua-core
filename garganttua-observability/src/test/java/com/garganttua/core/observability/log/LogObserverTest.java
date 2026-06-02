package com.garganttua.core.observability.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ErrorEvent;
import com.garganttua.core.observability.StartEvent;

class LogObserverTest {

    private static final UUID FIXED_ID = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000000");
    private static final Instant FIXED_TS = Instant.parse("2026-05-26T10:34:56.789Z");

    private static StartEvent start(String source) {
        return new StartEvent(FIXED_ID, FIXED_TS, source);
    }

    private static EndEvent end(String source, long ms, Integer code) {
        return new EndEvent(FIXED_ID, FIXED_TS, source, Duration.ofMillis(ms), code);
    }

    private static ErrorEvent error(String source, long ms, Throwable t) {
        return new ErrorEvent(FIXED_ID, FIXED_TS, source, Duration.ofMillis(ms), t);
    }

    // ---------- PlainTextEventFormatter ----------

    @Test
    @DisplayName("PlainText: StartEvent has type tag, source, short execId")
    void plainText_start() {
        String out = PlainTextEventFormatter.INSTANCE.format(start("workflow:foo"));
        assertTrue(out.contains("[START]"));
        assertTrue(out.contains("workflow:foo"));
        assertTrue(out.contains("a1b2c3d4"));
        assertFalse(out.endsWith("\n"), "formatters must not append newline");
    }

    @Test
    @DisplayName("PlainText: EndEvent includes duration and code")
    void plainText_end() {
        String out = PlainTextEventFormatter.INSTANCE.format(end("mapper:A->B", 23, 200));
        assertTrue(out.contains("[END  ]"));
        assertTrue(out.contains("23ms"));
        assertTrue(out.contains("code=200"));
    }

    @Test
    @DisplayName("PlainText: EndEvent without code omits the code field")
    void plainText_endNoCode() {
        String out = PlainTextEventFormatter.INSTANCE.format(end("mapper:A->B", 23, null));
        assertTrue(out.contains("23ms"));
        assertFalse(out.contains("code="));
    }

    @Test
    @DisplayName("PlainText: ErrorEvent includes exception class + message")
    void plainText_error() {
        String out = PlainTextEventFormatter.INSTANCE.format(
                error("runtime:foo", 45, new NullPointerException("boom")));
        assertTrue(out.contains("[ERROR]"));
        assertTrue(out.contains("NullPointerException"));
        assertTrue(out.contains("boom"));
    }

    // ---------- JsonLineEventFormatter ----------

    @Test
    @DisplayName("JsonLine: StartEvent has required fields, no trailing newline")
    void jsonLine_start() {
        String out = JsonLineEventFormatter.INSTANCE.format(start("workflow:foo"));
        assertTrue(out.startsWith("{"));
        assertTrue(out.endsWith("}"));
        assertTrue(out.contains("\"type\":\"start\""));
        assertTrue(out.contains("\"source\":\"workflow:foo\""));
        assertTrue(out.contains("\"executionId\":\"a1b2c3d4-0000-0000-0000-000000000000\""));
        assertTrue(out.contains("\"timestamp\":\"2026-05-26T10:34:56.789Z\""));
    }

    @Test
    @DisplayName("JsonLine: EndEvent surfaces durationMs and code as numbers")
    void jsonLine_end() {
        String out = JsonLineEventFormatter.INSTANCE.format(end("mapper:A->B", 23, 200));
        assertTrue(out.contains("\"type\":\"end\""));
        assertTrue(out.contains("\"durationMs\":23"));
        assertTrue(out.contains("\"code\":200"));
    }

    @Test
    @DisplayName("JsonLine: ErrorEvent surfaces errorClass + errorMessage + stacktrace")
    void jsonLine_error() {
        String out = JsonLineEventFormatter.INSTANCE.format(
                error("runtime:foo", 45, new RuntimeException("kaboom")));
        assertTrue(out.contains("\"type\":\"error\""));
        assertTrue(out.contains("\"errorClass\":\"java.lang.RuntimeException\""));
        assertTrue(out.contains("\"errorMessage\":\"kaboom\""));
        assertTrue(out.contains("\"stacktrace\":\""));
    }

    @Test
    @DisplayName("JsonLine: special characters in source are escaped")
    void jsonLine_escapesQuoteAndBackslash() {
        StartEvent ev = new StartEvent(FIXED_ID, FIXED_TS, "weird\"src\\with\nstuff");
        String out = JsonLineEventFormatter.INSTANCE.format(ev);
        assertTrue(out.contains("\\\""), "double-quote must be escaped");
        assertTrue(out.contains("\\\\"), "backslash must be escaped");
        assertTrue(out.contains("\\n"), "newline must be escaped");
    }

    @Test
    @DisplayName("JsonLine: control characters under 0x20 are unicode-escaped")
    void jsonLine_escapesControlChars() {
        StartEvent ev = new StartEvent(FIXED_ID, FIXED_TS, "srcend");
        String out = JsonLineEventFormatter.INSTANCE.format(ev);
        assertTrue(out.contains("\\u0001"));
    }

    // ---------- ConsoleLogObserver ----------

    @Test
    @DisplayName("ConsoleLogObserver writes one line per event to the supplied stream")
    void console_writesLines() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(buf, true, StandardCharsets.UTF_8);
        ConsoleLogObserver obs = ConsoleLogObserver.builder()
                .stream(stream)
                .formatter(JsonLineEventFormatter.INSTANCE)
                .build();

        obs.onEvent(start("workflow:foo"));
        obs.onEvent(end("workflow:foo", 10, 0));

        String[] lines = buf.toString(StandardCharsets.UTF_8).split("\\R");
        assertEquals(2, lines.length);
        assertTrue(lines[0].contains("\"type\":\"start\""));
        assertTrue(lines[1].contains("\"type\":\"end\""));
    }

    // ---------- FileLogObserver ----------

    @Test
    @DisplayName("FileLogObserver appends events as NDJSON lines")
    void file_appendsLines(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("events.ndjson");
        try (FileLogObserver obs = FileLogObserver.builder()
                .path(file)
                .formatter(JsonLineEventFormatter.INSTANCE)
                .append(true)
                .build()) {
            obs.onEvent(start("workflow:foo"));
            obs.onEvent(end("workflow:foo", 10, 0));
            obs.onEvent(error("workflow:foo", 12, new RuntimeException("oops")));
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("\"type\":\"start\""));
        assertTrue(lines.get(1).contains("\"type\":\"end\""));
        assertTrue(lines.get(2).contains("\"type\":\"error\""));
    }

    @Test
    @DisplayName("FileLogObserver creates parent directories")
    void file_createsParentDirs(@TempDir Path dir) throws Exception {
        Path nested = dir.resolve("a/b/c/events.ndjson");
        try (FileLogObserver obs = FileLogObserver.builder().path(nested).build()) {
            obs.onEvent(start("x"));
        }
        assertTrue(Files.exists(nested));
    }

    @Test
    @DisplayName("FileLogObserver append=false truncates on open")
    void file_truncate(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("events.ndjson");
        Files.writeString(file, "stale content\n", StandardCharsets.UTF_8);

        try (FileLogObserver obs = FileLogObserver.builder()
                .path(file).append(false).build()) {
            obs.onEvent(start("fresh"));
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"source\":\"fresh\""));
    }

    @Test
    @DisplayName("FileLogObserver close() is idempotent and post-close events are dropped silently")
    void file_idempotentClose(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("events.ndjson");
        FileLogObserver obs = FileLogObserver.builder().path(file).build();
        obs.onEvent(start("alive"));
        obs.close();
        obs.close();   // no exception
        assertTrue(obs.isClosed());

        // Post-close events are silently dropped.
        obs.onEvent(start("after-close"));
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"source\":\"alive\""));
    }

    @Test
    @DisplayName("FileLogObserver: 10 threads × 200 events all land without interleaving")
    void file_concurrentWrites(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("events.ndjson");
        int threads = 10;
        int loops = 200;
        AtomicInteger writeErrors = new AtomicInteger();

        try (FileLogObserver obs = FileLogObserver.builder().path(file).build()) {
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                pool.submit(() -> {
                    try {
                        for (int i = 0; i < loops; i++) {
                            obs.onEvent(start("thread-" + threadId + ":evt-" + i));
                        }
                    } catch (Exception e) {
                        writeErrors.incrementAndGet();
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        }

        assertEquals(0, writeErrors.get());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(threads * loops, lines.size(), "every event must produce exactly one line");
        // Every line should be a well-formed JSON object (starts with { ends with })
        for (String line : lines) {
            assertTrue(line.startsWith("{") && line.endsWith("}"),
                    "found malformed line (interleaving?): " + line);
        }
    }

    // ---------- Builder validation ----------

    @Test
    @DisplayName("FileLogObserver.builder().build() without path throws")
    void file_missingPath_throws() {
        assertThrows(IllegalStateException.class, () -> FileLogObserver.builder().build());
    }

    @Test
    @DisplayName("Null arguments to builders throw NPE")
    void nullArgs_throw() {
        assertThrows(NullPointerException.class,
                () -> ConsoleLogObserver.builder().stream(null));
        assertThrows(NullPointerException.class,
                () -> ConsoleLogObserver.builder().formatter(null));
        assertThrows(NullPointerException.class,
                () -> FileLogObserver.builder().path(null));
        assertThrows(NullPointerException.class,
                () -> FileLogObserver.builder().formatter(null));
    }

    // ---------- Sanity: PlainText shared instance is stateless ----------

    @Test
    @DisplayName("PlainText INSTANCE is reusable across events")
    void plainText_sharedInstance() {
        IEventFormatter f = PlainTextEventFormatter.INSTANCE;
        List<String> outs = new ArrayList<>();
        outs.add(f.format(start("a")));
        outs.add(f.format(end("a", 5, 0)));
        outs.add(f.format(error("a", 7, new RuntimeException("bad"))));
        assertEquals(3, outs.stream().distinct().count());
    }
}
