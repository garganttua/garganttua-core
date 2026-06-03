package com.garganttua.core.script.functions;

import java.util.concurrent.TimeUnit;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import jakarta.annotation.Nullable;
import com.garganttua.core.reflection.annotations.Reflected;

/** Mutex/synchronized script expression functions. Split out of ScriptFunctions; registered by FQN in ExpressionContextBuilder.FRAMEWORK_FUNCTION_CLASSES. */
@Reflected(queryAllDeclaredMethods = true)
public final class ScriptConcurrencyFunctions {
    private static final Logger log = Logger.getLogger(ScriptConcurrencyFunctions.class);
    private ScriptConcurrencyFunctions() {}

    /**
     * Executes an expression within a synchronized mutex lock.
     *
     * <p>This function provides distributed or local mutex synchronization
     * for critical sections. The mutex implementation (IMutex) determines
     * whether the lock is local (JVM) or distributed (e.g., Redis).</p>
     *
     * <p>The expression is passed lazily and executed only after the lock is acquired.</p>
     *
     * <p>Usage examples in script:</p>
     * <pre>
     * // Acquire mode - waits for lock with timeout
     * result &lt;- synchronized("my-mutex", $redisMutex, "acquire", seconds(30), myExpression())
     *
     * // TryAcquire mode - immediate, fails if lock not available
     * result &lt;- synchronized("my-mutex", $localMutex, "tryAcquire", milliseconds(0), myExpression())
     *
     * // With bean lookup for mutex
     * result &lt;- synchronized("order-lock", bean("myRedisMutex"), "acquire", seconds(10), processOrder())
     * </pre>
     *
     * @param mutexName the name/identifier for the mutex lock
     * @param mutex the IMutex implementation to use (local or distributed)
     * @param mode acquisition mode: "acquire" (wait with timeout) or "tryAcquire" (immediate)
     * @param timeoutMs timeout in milliseconds (use seconds(), milliseconds(), etc.)
     * @param expression the expression to execute within the lock (passed lazily as ISupplier)
     * @return the result of the expression execution
     * @throws ExpressionException if lock acquisition fails or expression execution fails
     */
    @Expression(name = "synchronized", description = "Executes expression within a mutex lock")
    public static Object synchronizedExec(
            @Nullable String mutexName,
            @Nullable IMutex mutex,
            @Nullable String mode,
            long timeoutMs,
            @Nullable ISupplier<?> expression) {

        log.debug("synchronized('{}', mutex, '{}', {}ms, ISupplier)",
                mutexName, mode, timeoutMs);

        // Validate parameters
        if (mutexName == null || mutexName.isBlank()) {
            throw new ExpressionException("synchronized: mutexName cannot be null or blank");
        }
        if (mutex == null) {
            throw new ExpressionException("synchronized: mutex cannot be null. " +
                    "Provide an IMutex instance (e.g., via bean() function)");
        }
        if (mode == null || mode.isBlank()) {
            throw new ExpressionException("synchronized: mode cannot be null. " +
                    "Use 'acquire' (wait with timeout) or 'tryAcquire' (immediate)");
        }
        if (expression == null) {
            return null;
        }

        // Determine acquisition strategy based on mode
        MutexStrategy strategy = createStrategy(mode, timeoutMs);

        try {
            return mutex.acquire(() -> {
                try {
                    return expression.supply().orElse(null);
                } catch (Exception e) {
                    throw new MutexException("Expression execution failed: " + e.getMessage(), e);
                }
            }, strategy);
        } catch (MutexException e) {
            throw new ExpressionException("synchronized: failed to acquire mutex '" + mutexName + "': " + e.getMessage());
        }
    }

    /**
     * Simplified synchronized execution with default acquire mode and no timeout (wait forever).
     *
     * <p>The expression is passed lazily and executed only after the lock is acquired.</p>
     *
     * <p>Usage in script:</p>
     * <pre>
     * result &lt;- sync("my-mutex", $mutex, myExpression())
     * </pre>
     *
     * @param mutexName the name/identifier for the mutex lock
     * @param mutex the IMutex implementation to use
     * @param expression the expression to execute within the lock (passed lazily as ISupplier)
     * @return the result of the expression execution
     */
    @Expression(name = "sync", description = "Simplified synchronized execution (waits forever for lock)")
    public static Object sync(
            @Nullable String mutexName,
            @Nullable IMutex mutex,
            @Nullable ISupplier<?> expression) {

        log.debug("sync('{}', mutex, ISupplier)", mutexName);

        if (mutexName == null || mutexName.isBlank()) {
            throw new ExpressionException("sync: mutexName cannot be null or blank");
        }
        if (mutex == null) {
            throw new ExpressionException("sync: mutex cannot be null");
        }
        if (expression == null) {
            return null;
        }

        try {
            return mutex.acquire(() -> {
                try {
                    return expression.supply().orElse(null);
                } catch (Exception e) {
                    throw new MutexException("Expression execution failed: " + e.getMessage(), e);
                }
            });
        } catch (MutexException e) {
            throw new ExpressionException("sync: failed to acquire mutex '" + mutexName + "': " + e.getMessage());
        }
    }

    /**
     * Creates a MutexStrategy based on the acquisition mode and timeout.
     *
     * @param mode "acquire" (with timeout) or "tryAcquire" (immediate)
     * @param timeoutMs timeout in milliseconds
     * @return the MutexStrategy
     */
    private static MutexStrategy createStrategy(String mode, long timeoutMs) {
        int waitTime;
        TimeUnit waitTimeUnit = TimeUnit.MILLISECONDS;

        switch (mode.toLowerCase(java.util.Locale.ROOT)) {
            case "acquire":
                // acquire mode: use timeout, or -1 if timeout is 0 (wait forever)
                waitTime = timeoutMs > 0 ? (int) timeoutMs : -1;
                break;
            case "tryacquire":
                // tryAcquire mode: immediate attempt, no waiting
                waitTime = 0;
                break;
            default:
                throw new ExpressionException("synchronized: unknown mode '" + mode + "'. " +
                        "Use 'acquire' or 'tryAcquire'");
        }

        // Create strategy with:
        // - waitTime/waitTimeUnit for timeout behavior
        // - No retries (0)
        // - No lease time enforcement (-1)
        return new MutexStrategy(
                waitTime,
                waitTimeUnit,
                0,                          // retries
                0,                          // retryInterval
                TimeUnit.MILLISECONDS,      // retryIntervalUnit
                -1,                         // leaseTime (-1 = no limit)
                TimeUnit.MILLISECONDS       // leaseTimeUnit
        );
    }
}
