package com.garganttua.core.mutex.functions;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.InterruptibleLeaseMutex;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexName;
import com.garganttua.core.mutex.context.MutexContext;
import com.garganttua.core.supply.ISupplier;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Expression functions for mutex-based synchronization.
 *
 * <p>
 * This class provides @Expression annotated functions for synchronizing
 * code execution using the garganttua mutex system. It uses the local
 * JVM mutex implementation (InterruptibleLeaseMutex).
 * </p>
 *
 * <h2>Usage Example (in .gs script)</h2>
 * <pre>
 * # Synchronize access to a shared resource
 * result <- sync("my-lock", someExpression)
 *
 * # With variable interpolation
 * lockName <- "user-lock"
 * value <- sync(@lockName, computeValue())
 * </pre>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>MutexContext must be initialized with an IMutexManager before use</li>
 *   <li>The mutex name is used to identify the lock across calls</li>
 *   <li>Same mutex name = same lock (thread-safe coordination)</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see MutexContext
 * @see IMutexManager
 * @see InterruptibleLeaseMutex
 */
@Slf4j
public final class MutexFunctions {

    private MutexFunctions() {
        // Utility class
    }

    /**
     * Synchronizes execution of an expression using a named mutex.
     *
     * <p>
     * This function acquires a mutex with the specified name, executes
     * the provided expression, and releases the mutex. The expression
     * is guaranteed to be executed in a mutually exclusive manner
     * across all threads using the same mutex name.
     * </p>
     *
     * @param mutexName the name of the mutex to acquire (converted to String via toString())
     * @param expression the expression (supplier) to execute while holding the mutex
     * @return the result of the expression evaluation
     * @throws ExpressionException if mutex manager is not available,
     *         mutex acquisition fails, or expression evaluation fails
     */
    @Expression(name = "sync", description = "Synchronizes execution using a local JVM mutex")
    public static Object sync(@Nullable Object mutexName, @Nullable ISupplier<?> expression) {
        String nameStr = mutexName == null ? null : mutexName.toString();
        log.atTrace().log("Entering sync(mutexName={}, expression={})", nameStr, expression);

        if (nameStr == null || nameStr.isBlank()) {
            throw new ExpressionException("sync: mutex name cannot be null or blank");
        }

        if (expression == null) {
            throw new ExpressionException("sync: expression cannot be null");
        }

        IMutexManager manager = MutexContext.get();
        if (manager == null) {
            throw new ExpressionException("sync: no MutexManager available in context. " +
                    "Ensure MutexContext.set() is called during initialization.");
        }

        try {
            // Create mutex name with default local mutex type
            MutexName name = new MutexName(InterruptibleLeaseMutex.class, nameStr);
            IMutex mutex = manager.mutex(name);

            log.atDebug().log("Acquiring mutex: {}", name);

            // Execute expression inside mutex
            Object result = mutex.acquire(() -> {
                log.atTrace().log("Mutex acquired, evaluating expression");
                return expression.supply().orElse(null);
            });

            log.atDebug().log("Mutex released, result: {}", result);
            return result;

        } catch (MutexException e) {
            log.atError().log("sync: mutex operation failed for '{}'", nameStr, e);
            throw new ExpressionException("sync: mutex operation failed - " + e.getMessage());
        } catch (Exception e) {
            log.atError().log("sync: expression evaluation failed", e);
            throw new ExpressionException("sync: expression evaluation failed - " + e.getMessage());
        }
    }
}
