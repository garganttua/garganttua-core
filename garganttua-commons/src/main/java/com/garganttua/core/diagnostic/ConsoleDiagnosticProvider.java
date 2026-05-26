package com.garganttua.core.diagnostic;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.garganttua.core.diagnostic.IDiagnostic.Level;

/**
 * Default {@link IDiagnosticProvider} — writes a single line per log call to
 * {@link System#err} (configurable). Format:
 * <pre>
 * 2026-05-26T10:34:56.789Z [LEVEL] short.class.Name - formatted message
 *     &lt;stack trace if throwable&gt;
 * </pre>
 *
 * <p>Level threshold is read from the {@code garganttua.diagnostic.level}
 * system property, defaulting to {@link Level#INFO}. Set to {@code TRACE} for
 * full verbosity, {@code OFF} (or {@code NONE}) to silence.
 *
 * <p>Thread-safe; one {@link IDiagnostic} cached per source class.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ConsoleDiagnosticProvider implements IDiagnosticProvider {

    private final PrintStream stream;
    private final Level threshold;
    private final ConcurrentMap<Class<?>, IDiagnostic> cache = new ConcurrentHashMap<>();

    public ConsoleDiagnosticProvider() {
        this(System.err, resolveThreshold());
    }

    public ConsoleDiagnosticProvider(PrintStream stream, Level threshold) {
        this.stream = Objects.requireNonNull(stream, "stream");
        this.threshold = Objects.requireNonNull(threshold, "threshold");
    }

    private static Level resolveThreshold() {
        String prop = System.getProperty("garganttua.diagnostic.level", "INFO");
        return switch (prop.trim().toUpperCase()) {
            case "TRACE" -> Level.TRACE;
            case "DEBUG" -> Level.DEBUG;
            case "INFO" -> Level.INFO;
            case "WARN", "WARNING" -> Level.WARN;
            case "ERROR" -> Level.ERROR;
            case "OFF", "NONE" -> null; // sentinel — handled in isEnabled
            default -> Level.INFO;
        };
    }

    @Override
    public IDiagnostic diagnosticFor(Class<?> source) {
        return this.cache.computeIfAbsent(source,
                cls -> new ConsoleDiagnostic(this.stream, this.threshold, cls));
    }

    /**
     * Per-class diagnostic instance. Package-private — outside callers should
     * use {@link Diagnostics#of(Class)}.
     */
    static final class ConsoleDiagnostic implements IDiagnostic {

        private final PrintStream stream;
        private final Level threshold;
        private final String shortName;

        ConsoleDiagnostic(PrintStream stream, Level threshold, Class<?> source) {
            this.stream = stream;
            this.threshold = threshold;
            this.shortName = shortNameOf(source);
        }

        @Override
        public boolean isEnabled(Level level) {
            return this.threshold != null && level.ordinal() >= this.threshold.ordinal();
        }

        @Override
        public void log(Level level, String format, Object... args) {
            if (!isEnabled(level)) {
                return;
            }
            DiagnosticFormatter.FormatArgs split = DiagnosticFormatter.split(args);
            String msg = DiagnosticFormatter.format(format, split.args());
            String line = Instant.now() + " [" + level.name() + "] " + this.shortName + " - " + msg;
            // PrintStream.println is atomic for the whole line; safe under
            // concurrent producers.
            synchronized (this.stream) {
                this.stream.println(line);
                if (split.throwable() != null) {
                    split.throwable().printStackTrace(this.stream);
                }
            }
        }

        private static String shortNameOf(Class<?> cls) {
            String n = cls.getName();
            int dot = n.lastIndexOf('.');
            if (dot < 0) {
                return n;
            }
            // Keep last package segment + class — e.g. "core.runtime.Runtime"
            int prev = n.lastIndexOf('.', dot - 1);
            return prev < 0 ? n : n.substring(prev + 1);
        }
    }
}
