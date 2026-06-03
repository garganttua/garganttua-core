package com.garganttua.core.observability;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.garganttua.core.observability.ObservableContextHolder.Session;
import com.garganttua.core.reflection.IClass;

/**
 * Observable logging facade — every log call is fired as a {@link LogEvent} to
 * observers. Zero external dependency; replaces the former {@code IDiagnostic}.
 *
 * <p>Obtain one per class, held in a {@code static final} field:
 * <pre>{@code
 * private static final Logger log = Logger.getLogger(MyService.class);
 *
 * log.debug("processing item {}", item);
 * log.error("operation failed", exception);
 * }</pre>
 *
 * <h2>Pure event source</h2>
 * A {@code Logger} does NOT write anywhere on its own — it only emits
 * {@link LogEvent}s. When no observer is registered (on the logger, on the
 * {@linkplain #global() global registry}, or on the currently-bound
 * {@link ObservableContextHolder} session) a log call is a cheap no-op. Attach a
 * {@code ConsoleLogObserver} / {@code FileLogObserver} (in
 * {@code garganttua-observability}) to actually see output.
 *
 * <h2>Correlation</h2>
 * When an execution session is bound on the current thread (a running workflow,
 * runtime, mapper…), the emitted {@link LogEvent} adopts that session's
 * {@code executionId} and is also fired to the session's registry, so a single
 * observer at the top of the call chain sees the logs correlated with the
 * surrounding execution. With no session bound, the event carries the nil UUID.
 *
 * <h2>Level threshold</h2>
 * Calls below the {@code garganttua.log.level} system-property threshold
 * (default {@code INFO}; accepts {@code TRACE|DEBUG|INFO|WARN|ERROR|OFF}) are
 * short-circuited before any event is built — keeping the cost on filtered-out
 * {@code trace}/{@code debug} calls negligible.
 *
 * <h2>Throwable handling — SLF4J-style convention</h2>
 * When the LAST varargs argument is a {@link Throwable}, it is treated as the
 * cause: it is NOT consumed by a {@code {}} placeholder and is attached as the
 * event's {@link LogEvent#payload()}.
 *
 * <p>Thread-safe.
 *
 * @since 2.0.0-ALPHA02
 */
public final class Logger implements IObservable {

    // NOTE: CACHE must be initialized BEFORE GLOBAL. Creating GLOBAL triggers
    // ObservableRegistry.<clinit>, whose own `static final Logger log` field calls
    // Logger.getLogger(...) — which needs CACHE. Declaring CACHE first breaks this
    // static-init cycle (otherwise CACHE is still null → NPE → NoClassDefFoundError).
    private static final ConcurrentMap<String, Logger> CACHE = new ConcurrentHashMap<>();

    /** Global registry — one observer attached here sees every logger's events. */
    private static final ObservableRegistry GLOBAL = new ObservableRegistry();

    private static final UUID NO_EXECUTION = new UUID(0L, 0L);

    /** Ordinal of the configured threshold; {@link Integer#MAX_VALUE} means OFF. */
    private static final int THRESHOLD = resolveThreshold();

    private final String name;
    private final ObservableRegistry registry = new ObservableRegistry();

    private Logger(String name) {
        this.name = name;
    }

    // ------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------

    /**
     * @return the logger bound to the given source class descriptor. The logger
     *         name is {@link IClass#getName()}.
     */
    public static Logger getLogger(IClass<?> source) {
        return getLogger(source.getName());
    }

    /**
     * Name-only factory — extracts {@link Class#getName()} without touching the
     * reflection subsystem, so it is safe to call from {@code static} field
     * initializers on a cold JVM (no {@code IReflection} required).
     */
    public static Logger getLogger(Class<?> source) {
        return getLogger(source.getName());
    }

    /**
     * @return the (cached) logger with the given name.
     */
    public static Logger getLogger(String name) {
        return CACHE.computeIfAbsent(Objects.requireNonNull(name, "name"), Logger::new);
    }

    /**
     * @return the global registry every logger fires to — attach one observer
     *         here to receive log events from all loggers.
     */
    public static IObservable global() {
        return GLOBAL;
    }

    /**
     * @return the name this logger was created with (typically a fully qualified
     *         class name).
     */
    public String name() {
        return this.name;
    }

    // ------------------------------------------------------------------
    // Per-logger observation
    // ------------------------------------------------------------------

    @Override
    public void addObserver(IObserver<ObservableEvent> observer) {
        this.registry.addObserver(observer);
    }

    @Override
    public void removeObserver(IObserver<ObservableEvent> observer) {
        this.registry.removeObserver(observer);
    }

    // ------------------------------------------------------------------
    // Logging API
    // ------------------------------------------------------------------

    /**
     * @return whether messages at {@code level} could be emitted (threshold
     *         passes AND at least one registry has an observer). Useful only
     *         when building the message itself is expensive.
     */
    public boolean isEnabled(LogEvent.Level level) {
        return level.ordinal() >= THRESHOLD && hasAnyObserver(ObservableContextHolder.current());
    }

    /**
     * Emit a {@code TRACE}-level message. See {@link #log(LogEvent.Level, String, Object...)}
     * for placeholder and {@link Throwable} handling.
     *
     * @param format SLF4J-style message with {@code {}} placeholders
     * @param args   substitution arguments; a trailing {@link Throwable} is the cause
     */
    public void trace(String format, Object... args) {
        log(LogEvent.Level.TRACE, format, args);
    }

    /**
     * Emit a {@code DEBUG}-level message. See {@link #log(LogEvent.Level, String, Object...)}
     * for placeholder and {@link Throwable} handling.
     *
     * @param format SLF4J-style message with {@code {}} placeholders
     * @param args   substitution arguments; a trailing {@link Throwable} is the cause
     */
    public void debug(String format, Object... args) {
        log(LogEvent.Level.DEBUG, format, args);
    }

    /**
     * Emit an {@code INFO}-level message. See {@link #log(LogEvent.Level, String, Object...)}
     * for placeholder and {@link Throwable} handling.
     *
     * @param format SLF4J-style message with {@code {}} placeholders
     * @param args   substitution arguments; a trailing {@link Throwable} is the cause
     */
    public void info(String format, Object... args) {
        log(LogEvent.Level.INFO, format, args);
    }

    /**
     * Emit a {@code WARN}-level message. See {@link #log(LogEvent.Level, String, Object...)}
     * for placeholder and {@link Throwable} handling.
     *
     * @param format SLF4J-style message with {@code {}} placeholders
     * @param args   substitution arguments; a trailing {@link Throwable} is the cause
     */
    public void warn(String format, Object... args) {
        log(LogEvent.Level.WARN, format, args);
    }

    /**
     * Emit an {@code ERROR}-level message. See {@link #log(LogEvent.Level, String, Object...)}
     * for placeholder and {@link Throwable} handling.
     *
     * @param format SLF4J-style message with {@code {}} placeholders
     * @param args   substitution arguments; a trailing {@link Throwable} is the cause
     */
    public void error(String format, Object... args) {
        log(LogEvent.Level.ERROR, format, args);
    }

    /**
     * Emit a message at the given level. No-op when filtered by threshold or
     * when no observer is listening. If the last element of {@code args} is a
     * {@link Throwable}, it becomes the event payload rather than a substitution.
     */
    public void log(LogEvent.Level level, String format, Object... args) {
        if (level.ordinal() < THRESHOLD) {
            return;
        }
        Session session = ObservableContextHolder.current();
        if (!hasAnyObserver(session)) {
            return;
        }

        Object[] substitution = args;
        Throwable throwable = null;
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable t) {
            throwable = t;
            substitution = new Object[args.length - 1];
            System.arraycopy(args, 0, substitution, 0, substitution.length);
        }

        String message = format(format, substitution);
        UUID executionId = (session != null) ? session.executionId() : NO_EXECUTION;
        LogEvent event = new LogEvent(executionId, Instant.now(), this.name, level, message, throwable);

        GLOBAL.fire(event);
        this.registry.fire(event);
        if (session != null) {
            ObservableRegistry sessionRegistry = session.registry();
            if (sessionRegistry != null && sessionRegistry != GLOBAL && sessionRegistry != this.registry) {
                sessionRegistry.fire(event);
            }
        }
    }

    private boolean hasAnyObserver(Session session) {
        if (GLOBAL.hasObservers() || this.registry.hasObservers()) {
            return true;
        }
        if (session != null) {
            ObservableRegistry sessionRegistry = session.registry();
            return sessionRegistry != null && sessionRegistry.hasObservers();
        }
        return false;
    }

    // ------------------------------------------------------------------
    // {}-placeholder formatting (SLF4J-compatible)
    // ------------------------------------------------------------------

    private static String format(String format, Object[] args) {
        if (format == null) {
            return "null";
        }
        if (args == null || args.length == 0) {
            return format;
        }
        StringBuilder out = new StringBuilder(format.length() + args.length * 8);
        int argIdx = 0;
        int i = 0;
        final int n = format.length();
        while (i < n) {
            char c = format.charAt(i);
            if (c == '{' && i + 1 < n && format.charAt(i + 1) == '}' && argIdx < args.length) {
                out.append(stringOf(args[argIdx++]));
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String stringOf(Object o) {
        if (o == null) {
            return "null";
        }
        if (o.getClass().isArray()) {
            if (o instanceof Object[] arr) {
                return java.util.Arrays.deepToString(arr);
            }
            if (o instanceof int[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof long[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof double[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof float[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof boolean[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof byte[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof short[] arr) return java.util.Arrays.toString(arr);
            if (o instanceof char[] arr) return java.util.Arrays.toString(arr);
        }
        return o.toString();
    }

    private static int resolveThreshold() {
        String prop = System.getProperty("garganttua.log.level", "INFO");
        return switch (prop.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "TRACE" -> LogEvent.Level.TRACE.ordinal();
            case "DEBUG" -> LogEvent.Level.DEBUG.ordinal();
            case "INFO" -> LogEvent.Level.INFO.ordinal();
            case "WARN", "WARNING" -> LogEvent.Level.WARN.ordinal();
            case "ERROR" -> LogEvent.Level.ERROR.ordinal();
            case "OFF", "NONE" -> Integer.MAX_VALUE;
            default -> LogEvent.Level.INFO.ordinal();
        };
    }
}
