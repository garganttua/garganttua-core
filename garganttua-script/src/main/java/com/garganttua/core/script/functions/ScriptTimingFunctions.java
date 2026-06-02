package com.garganttua.core.script.functions;

import java.util.concurrent.TimeUnit;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import jakarta.annotation.Nullable;
import com.garganttua.core.reflection.annotations.Reflected;

/** Time-measurement and time-unit script expression functions. Split out of ScriptFunctions; registered by FQN in ExpressionContextBuilder.FRAMEWORK_FUNCTION_CLASSES. */
@Reflected(queryAllDeclaredMethods = true)
public final class ScriptTimingFunctions {
    private static final Logger log = Logger.getLogger(ScriptTimingFunctions.class);
    private ScriptTimingFunctions() {}

    /**
     * Measures the execution time of an expression.
     *
     * <p>The expression is passed lazily as an ISupplier, meaning it is NOT evaluated
     * before being passed to this function. The execution time is measured when the
     * supplier is invoked.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>{@code
     * // Measure time of any expression (expression is passed lazily)
     * elapsed <- time(print("hello"))
     * elapsed <- time(someExpensiveOperation())
     *
     * // Measure time of a stored expression
     * expr = someExpensiveOperation()
     * elapsed <- time(@expr)
     * print(concatenate("Operation took ", string(elapsed), "ms"))
     * }</pre>
     *
     * @param expression the expression to measure (passed lazily as ISupplier)
     * @return the elapsed time in milliseconds
     */
    @Expression(name = "time", description = "Measures execution time of an expression in milliseconds")
    public static long time(@Nullable ISupplier<?> expression) {
        log.debug("time(ISupplier)");

        if (expression == null) {
            return 0L;
        }

        long startTime = System.currentTimeMillis();

        try {
            expression.supply();
        } catch (Exception e) {
            // Still return elapsed time even on failure
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("time: expression failed after {}ms: {}", elapsed, e.getMessage());
            throw new ExpressionException("time: expression execution failed: " + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("time: execution completed in {}ms", elapsed);
        return elapsed;
    }

    /**
     * Measures the execution time of an expression and returns both time and result.
     *
     * <p>Returns an array where [0] is the elapsed time in milliseconds and [1] is the result.</p>
     *
     * <p>The expression is passed lazily as an ISupplier, meaning it is NOT evaluated
     * before being passed to this function.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>{@code
     * // Measure and get result
     * result <- timeWithResult(someOperation())
     * // result[0] = elapsed time, result[1] = operation result
     *
     * // Or with stored expression
     * expr = someOperation()
     * result <- timeWithResult(@expr)
     * }</pre>
     *
     * @param expression the expression to measure (passed lazily as ISupplier)
     * @return array of [elapsedMs, result]
     */
    @Expression(name = "timeWithResult", description = "Measures execution time and returns [timeMs, result]")
    public static Object[] timeWithResult(@Nullable ISupplier<?> expression) {
        log.debug("timeWithResult(ISupplier)");

        if (expression == null) {
            return new Object[] { 0L, null };
        }

        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            result = expression.supply().orElse(null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            throw new ExpressionException("timeWithResult: expression execution failed after " + elapsed + "ms: " + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("timeWithResult: execution completed in {}ms", elapsed);
        return new Object[] { elapsed, result };
    }

    // ========== Time Unit Functions ==========

    /**
     * Identity conversion treating {@code value} as milliseconds.
     *
     * @param value the duration in milliseconds
     * @return {@code value}
     */
    @Expression(name = "milliseconds", description = "Returns the value as milliseconds (identity function)")
    public static long milliseconds(long value) {
        return value;
    }

    /**
     * Identity conversion treating {@code value} as milliseconds.
     *
     * @param value the duration in milliseconds
     * @return {@code value}
     */
    @Expression(name = "milliseconds", description = "Returns the value as milliseconds (identity function)")
    public static long milliseconds(int value) {
        return value;
    }

    /**
     * Converts seconds to milliseconds.
     *
     * @param value the duration in seconds
     * @return the equivalent milliseconds
     */
    @Expression(name = "seconds", description = "Converts seconds to milliseconds")
    public static long seconds(long value) {
        return TimeUnit.SECONDS.toMillis(value);
    }

    /**
     * Converts seconds to milliseconds.
     *
     * @param value the duration in seconds
     * @return the equivalent milliseconds
     */
    @Expression(name = "seconds", description = "Converts seconds to milliseconds")
    public static long seconds(int value) {
        return TimeUnit.SECONDS.toMillis(value);
    }

    /**
     * Converts minutes to milliseconds.
     *
     * @param value the duration in minutes
     * @return the equivalent milliseconds
     */
    @Expression(name = "minutes", description = "Converts minutes to milliseconds")
    public static long minutes(long value) {
        return TimeUnit.MINUTES.toMillis(value);
    }

    /**
     * Converts minutes to milliseconds.
     *
     * @param value the duration in minutes
     * @return the equivalent milliseconds
     */
    @Expression(name = "minutes", description = "Converts minutes to milliseconds")
    public static long minutes(int value) {
        return TimeUnit.MINUTES.toMillis(value);
    }

    /**
     * Converts hours to milliseconds.
     *
     * @param value the duration in hours
     * @return the equivalent milliseconds
     */
    @Expression(name = "hours", description = "Converts hours to milliseconds")
    public static long hours(long value) {
        return TimeUnit.HOURS.toMillis(value);
    }

    /**
     * Converts hours to milliseconds.
     *
     * @param value the duration in hours
     * @return the equivalent milliseconds
     */
    @Expression(name = "hours", description = "Converts hours to milliseconds")
    public static long hours(int value) {
        return TimeUnit.HOURS.toMillis(value);
    }

}
