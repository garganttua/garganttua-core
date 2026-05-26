package com.garganttua.core.diagnostic;

/**
 * Minimal logging facade — zero external dependency.
 *
 * <p>Drop-in replacement for the subset of SLF4J that {@code garganttua-core}
 * actually uses: leveled log lines with {@code {}}-placeholder format
 * strings and an optional trailing {@link Throwable}. No MDC, no markers, no
 * fluent builders. Everything you need to migrate from
 * {@code log.debug("msg {}", arg)} to {@code log.debug("msg {}", arg)}
 * one-for-one.
 *
 * <p>Implementations are obtained via {@link Diagnostics#of(Class)}:
 * <pre>{@code
 * private static final IDiagnostic log = Diagnostics.of(MyService.class);
 *
 * log.debug("processing item {}", item);
 * log.error("operation failed", exception);
 * }</pre>
 *
 * <p>The default provider writes a single line per call to {@link System#err}
 * with a timestamp + level + class + formatted message. Apps can install a
 * custom {@link IDiagnosticProvider} (e.g. one that emits each call as an
 * observability event, or one that buffers to NDJSON) via
 * {@link Diagnostics#setProvider(IDiagnosticProvider)}.
 *
 * <h2>Throwable handling — SLF4J-style convention</h2>
 * When the LAST varargs argument is a {@link Throwable}, it is treated as the
 * cause to print/attach and NOT as a format-string placeholder substitution.
 * This matches SLF4J's behavior so migration is a literal API swap.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IDiagnostic {

    /**
     * @return whether messages at this {@code level} would be emitted. Useful
     *         only when computing the message itself is expensive (otherwise
     *         the level filter inside {@link #log} short-circuits cheaply).
     */
    boolean isEnabled(Level level);

    /**
     * Emit a message at the given level. If the last element of {@code args}
     * is a {@link Throwable}, it is treated as the cause.
     */
    void log(Level level, String format, Object... args);

    default void trace(String format, Object... args) {
        if (isEnabled(Level.TRACE)) log(Level.TRACE, format, args);
    }

    default void debug(String format, Object... args) {
        if (isEnabled(Level.DEBUG)) log(Level.DEBUG, format, args);
    }

    default void info(String format, Object... args) {
        if (isEnabled(Level.INFO)) log(Level.INFO, format, args);
    }

    default void warn(String format, Object... args) {
        if (isEnabled(Level.WARN)) log(Level.WARN, format, args);
    }

    default void error(String format, Object... args) {
        if (isEnabled(Level.ERROR)) log(Level.ERROR, format, args);
    }

    /**
     * Severity levels, ordered from most verbose to most severe.
     */
    enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
