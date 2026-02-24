package com.garganttua.core.script.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.garganttua.core.expression.annotations.Expression;

import jakarta.annotation.Nullable;

/**
 * Logging expression functions for Garganttua Script.
 *
 * <p>Provides SLF4J-based logging at all standard levels (TRACE, DEBUG, INFO, WARN, ERROR).
 * All functions use a dedicated logger named {@code garganttua.script} so that script-level
 * logging can be configured independently from framework logging.</p>
 *
 * <p>Usage in script:</p>
 * <pre>
 * log_info("Application started")
 * log_debug(format("Processing item %s", @itemId))
 * log_warn(concatenate("Slow response: ", string(@elapsed), "ms"))
 * log_error("Failed to connect")
 * </pre>
 */
public class LogFunctions {

    private static final Logger SCRIPT_LOGGER = LoggerFactory.getLogger("garganttua.script");

    private LogFunctions() {
    }

    // ========== TRACE ==========

    /**
     * Logs a message at TRACE level.
     *
     * @param message the message to log
     * @return the logged message string
     */
    @Expression(name = "log_trace", description = "Logs a message at TRACE level")
    public static String logTrace(@Nullable Object message) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.trace(msg);
        return msg;
    }

    /**
     * Logs a formatted message at TRACE level with one argument.
     *
     * <p>Usage: {@code log_trace("Processing item {}", @itemId)}</p>
     *
     * @param message the message pattern (use {} for placeholders)
     * @param arg the argument
     * @return the message pattern string
     */
    @Expression(name = "log_trace", description = "Logs a formatted message at TRACE level with 1 argument")
    public static String logTrace(@Nullable Object message, @Nullable Object arg) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.trace(msg, arg);
        return msg;
    }

    // ========== DEBUG ==========

    /**
     * Logs a message at DEBUG level.
     *
     * @param message the message to log
     * @return the logged message string
     */
    @Expression(name = "log_debug", description = "Logs a message at DEBUG level")
    public static String logDebug(@Nullable Object message) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.debug(msg);
        return msg;
    }

    /**
     * Logs a formatted message at DEBUG level with one argument.
     *
     * @param message the message pattern (use {} for placeholders)
     * @param arg the argument
     * @return the message pattern string
     */
    @Expression(name = "log_debug", description = "Logs a formatted message at DEBUG level with 1 argument")
    public static String logDebug(@Nullable Object message, @Nullable Object arg) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.debug(msg, arg);
        return msg;
    }

    // ========== INFO ==========

    /**
     * Logs a message at INFO level.
     *
     * @param message the message to log
     * @return the logged message string
     */
    @Expression(name = "log_info", description = "Logs a message at INFO level")
    public static String logInfo(@Nullable Object message) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.info(msg);
        return msg;
    }

    /**
     * Logs a formatted message at INFO level with one argument.
     *
     * @param message the message pattern (use {} for placeholders)
     * @param arg the argument
     * @return the message pattern string
     */
    @Expression(name = "log_info", description = "Logs a formatted message at INFO level with 1 argument")
    public static String logInfo(@Nullable Object message, @Nullable Object arg) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.info(msg, arg);
        return msg;
    }

    // ========== WARN ==========

    /**
     * Logs a message at WARN level.
     *
     * @param message the message to log
     * @return the logged message string
     */
    @Expression(name = "log_warn", description = "Logs a message at WARN level")
    public static String logWarn(@Nullable Object message) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.warn(msg);
        return msg;
    }

    /**
     * Logs a formatted message at WARN level with one argument.
     *
     * @param message the message pattern (use {} for placeholders)
     * @param arg the argument
     * @return the message pattern string
     */
    @Expression(name = "log_warn", description = "Logs a formatted message at WARN level with 1 argument")
    public static String logWarn(@Nullable Object message, @Nullable Object arg) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.warn(msg, arg);
        return msg;
    }

    // ========== ERROR ==========

    /**
     * Logs a message at ERROR level.
     *
     * @param message the message to log
     * @return the logged message string
     */
    @Expression(name = "log_error", description = "Logs a message at ERROR level")
    public static String logError(@Nullable Object message) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.error(msg);
        return msg;
    }

    /**
     * Logs a formatted message at ERROR level with one argument.
     *
     * @param message the message pattern (use {} for placeholders)
     * @param arg the argument
     * @return the message pattern string
     */
    @Expression(name = "log_error", description = "Logs a formatted message at ERROR level with 1 argument")
    public static String logError(@Nullable Object message, @Nullable Object arg) {
        String msg = message == null ? "null" : message.toString();
        SCRIPT_LOGGER.error(msg, arg);
        return msg;
    }
}
