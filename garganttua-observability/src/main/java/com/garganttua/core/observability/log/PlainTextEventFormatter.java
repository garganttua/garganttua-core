package com.garganttua.core.observability.log;

import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ErrorEvent;
import com.garganttua.core.observability.LogEvent;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;

/**
 * Human-readable single-line formatter — designed for console output and
 * {@code tail -f} dev usage.
 *
 * <p>Format examples:
 * <pre>
 * 2026-05-26T10:34:56.789Z [START] runtime:script:step:step-0-user (exec=a1b2…)
 * 2026-05-26T10:34:56.812Z [END  ] runtime:script:step:step-0-user (exec=a1b2…, 23ms, code=0)
 * 2026-05-26T10:34:56.834Z [ERROR] runtime:script:step:step-0-user (exec=a1b2…, 45ms): NullPointerException: foo
 * </pre>
 *
 * <p>The executionId is truncated to its first 8 hex chars for visual scan-
 * ability; the full UUID still flows through every event for machine
 * correlation.
 *
 * <p>Thread-safe and stateless — share a single instance across observers.
 *
 * @since 2.0.0-ALPHA02
 */
public final class PlainTextEventFormatter implements IEventFormatter {

    /** Convenience shared instance — the formatter is stateless. */
    public static final PlainTextEventFormatter INSTANCE = new PlainTextEventFormatter();

    @Override
    public String format(ObservableEvent event) {
        StringBuilder sb = new StringBuilder(96);
        sb.append(event.timestamp());
        sb.append(' ');
        switch (event) {
            case StartEvent s -> sb.append("[START]");
            case EndEvent e -> sb.append("[END  ]");
            case ErrorEvent x -> sb.append("[ERROR]");
            case LogEvent l -> sb.append("[LOG  ]");
        }
        sb.append(' ');
        sb.append(event.source());
        sb.append(" (exec=").append(shortId(event.executionId().toString()));
        switch (event) {
            case StartEvent s -> sb.append(')');
            case EndEvent e -> {
                sb.append(", ").append(e.duration().toMillis()).append("ms");
                if (e.code() != null) {
                    sb.append(", code=").append(e.code());
                }
                sb.append(')');
            }
            case ErrorEvent x -> {
                sb.append(", ").append(x.duration().toMillis()).append("ms");
                sb.append(')');
                if (x.failure() != null) {
                    sb.append(": ").append(x.failure().getClass().getSimpleName());
                    String msg = x.failure().getMessage();
                    if (msg != null && !msg.isEmpty()) {
                        sb.append(": ").append(msg);
                    }
                }
            }
            case LogEvent l -> {
                if (l.level() != null) {
                    sb.append(", ").append(l.level());
                }
                sb.append(')');
                if (l.message() != null && !l.message().isEmpty()) {
                    sb.append(": ").append(l.message());
                }
            }
        }
        if (event.payload() != null) {
            sb.append(" payload=").append(event.payload());
        }
        return sb.toString();
    }

    private static String shortId(String uuid) {
        int dash = uuid.indexOf('-');
        return dash > 0 ? uuid.substring(0, dash) + "…" : uuid;
    }
}
