package com.garganttua.core.observability.log;

import com.garganttua.core.observability.ObservableEvent;

/**
 * Formats an {@link ObservableEvent} as a single-line string ready to be
 * appended to a log sink (console, file, network appender, …).
 *
 * <p>Implementations MUST NOT include trailing newline — the observer that
 * consumes the formatted string is responsible for line termination.
 *
 * <p>Two default implementations ship with garganttua-observability:
 * <ul>
 *   <li>{@link PlainTextEventFormatter} — human-readable single line for
 *       console output and {@code tail -f} usage.</li>
 *   <li>{@link JsonLineEventFormatter} — NDJSON (newline-delimited JSON),
 *       ingestible by Elasticsearch / Loki / Splunk / etc. without any
 *       further transformation.</li>
 * </ul>
 *
 * <p>Custom formatters live in user code or in dedicated binding modules
 * (e.g. {@code garganttua-observability-elasticsearch}) — the contract here
 * is intentionally minimal to keep {@code garganttua-observability} free of
 * external dependencies.
 *
 * @since 2.0.0-ALPHA02
 */
@FunctionalInterface
public interface IEventFormatter {

    /**
     * Format the event into a single-line string. Implementations should be
     * thread-safe and side-effect-free — the same observer instance may be
     * invoked from multiple producer threads concurrently.
     *
     * @param event the event to format (never {@code null})
     * @return a formatted line, without trailing newline
     */
    String format(ObservableEvent event);
}
