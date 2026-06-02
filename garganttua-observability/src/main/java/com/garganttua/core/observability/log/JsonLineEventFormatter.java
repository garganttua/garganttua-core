package com.garganttua.core.observability.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ErrorEvent;
import com.garganttua.core.observability.LogEvent;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;

/**
 * NDJSON (newline-delimited JSON) formatter — one object per event, no
 * trailing newline (the observer adds it). The output is directly ingestible
 * by Elasticsearch / Filebeat / Loki / Splunk / etc. without any further
 * transformation.
 *
 * <p>Common fields (always present):
 * <ul>
 *   <li>{@code timestamp} (ISO-8601)</li>
 *   <li>{@code executionId} (UUID string)</li>
 *   <li>{@code source} (hierarchical string)</li>
 *   <li>{@code type} (one of {@code start}, {@code end}, {@code error}, {@code log})</li>
 * </ul>
 *
 * <p>End/error-only fields: {@code durationMs}, {@code code} (end), {@code
 * errorClass}, {@code errorMessage}, {@code stacktrace} (error). Log-only fields:
 * {@code level}, {@code message}. The optional {@code payload} field (the
 * event's {@code toString()}) is appended to any event type when present.
 *
 * <p>Pure JVM, no external dependency — escape logic is inlined.
 * Thread-safe and stateless.
 *
 * @since 2.0.0-ALPHA02
 */
public final class JsonLineEventFormatter implements IEventFormatter {

    /** Convenience shared instance — the formatter is stateless. */
    public static final JsonLineEventFormatter INSTANCE = new JsonLineEventFormatter();

    @Override
    public String format(ObservableEvent event) {
        StringBuilder sb = new StringBuilder(160);
        sb.append('{');
        appendString(sb, "timestamp", event.timestamp().toString());
        sb.append(',');
        appendString(sb, "executionId", event.executionId().toString());
        sb.append(',');
        appendString(sb, "source", event.source());
        sb.append(',');
        switch (event) {
            case StartEvent s -> appendString(sb, "type", "start");
            case EndEvent e -> {
                appendString(sb, "type", "end");
                sb.append(',');
                appendNumber(sb, "durationMs", e.duration().toMillis());
                if (e.code() != null) {
                    sb.append(',');
                    appendNumber(sb, "code", e.code());
                }
            }
            case ErrorEvent x -> {
                appendString(sb, "type", "error");
                sb.append(',');
                appendNumber(sb, "durationMs", x.duration().toMillis());
                Throwable f = x.failure();
                if (f != null) {
                    sb.append(',');
                    appendString(sb, "errorClass", f.getClass().getName());
                    if (f.getMessage() != null) {
                        sb.append(',');
                        appendString(sb, "errorMessage", f.getMessage());
                    }
                    sb.append(',');
                    appendString(sb, "stacktrace", stackTrace(f));
                }
            }
            case LogEvent l -> {
                appendString(sb, "type", "log");
                if (l.level() != null) {
                    sb.append(',');
                    appendString(sb, "level", l.level().name());
                }
                if (l.message() != null) {
                    sb.append(',');
                    appendString(sb, "message", l.message());
                }
            }
        }
        if (event.payload() != null) {
            sb.append(',');
            appendString(sb, "payload", String.valueOf(event.payload()));
        }
        sb.append('}');
        return sb.toString();
    }

    private static void appendString(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"');
            escape(sb, value);
            sb.append('"');
        }
    }

    private static void appendNumber(StringBuilder sb, String key, long value) {
        sb.append('"').append(key).append("\":").append(value);
    }

    private static void escape(StringBuilder sb, String s) {
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
