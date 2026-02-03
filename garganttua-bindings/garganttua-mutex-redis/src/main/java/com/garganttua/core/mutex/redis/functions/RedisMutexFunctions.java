package com.garganttua.core.mutex.redis.functions;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexName;
import com.garganttua.core.mutex.context.MutexContext;
import com.garganttua.core.mutex.redis.RedisMutex;
import com.garganttua.core.supply.ISupplier;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Expression functions for Redis-based distributed mutex synchronization.
 *
 * <p>
 * This class provides @Expression annotated functions for synchronizing
 * code execution using Redis-based distributed locks. This allows
 * coordination across multiple JVMs/processes/servers.
 * </p>
 *
 * <h2>Usage Example (in .gs script)</h2>
 * <pre>
 * # Synchronize across distributed systems
 * result <- syncRedis("distributed-lock", someExpression)
 *
 * # With variable interpolation
 * lockName <- "user-lock"
 * value <- syncRedis(@lockName, computeValue())
 * </pre>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>MutexContext must be initialized with an IMutexManager that has RedisMutexFactory</li>
 *   <li>Redis server must be accessible</li>
 *   <li>The mutex name is used to identify the lock across all connected processes</li>
 * </ul>
 *
 * <h2>Distributed Lock Behavior</h2>
 * <ul>
 *   <li>Locks are held in Redis and visible across all connected clients</li>
 *   <li>If a process crashes, the lock will eventually expire (based on lease time)</li>
 *   <li>More expensive than local mutex but provides cross-process coordination</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see MutexContext
 * @see IMutexManager
 * @see RedisMutex
 */
@Slf4j
public final class RedisMutexFunctions {

    private RedisMutexFunctions() {
        // Utility class
    }

    /**
     * Synchronizes execution of an expression using a Redis-based distributed mutex.
     *
     * <p>
     * This function acquires a distributed mutex via Redis, executes
     * the provided expression, and releases the mutex. The expression
     * is guaranteed to be executed in a mutually exclusive manner
     * across all processes/JVMs using the same Redis server and mutex name.
     * </p>
     *
     * @param mutexName the name of the distributed mutex to acquire
     * @param expression the expression (supplier) to execute while holding the mutex
     * @return the result of the expression evaluation
     * @throws ExpressionException if mutex manager is not available,
     *         RedisMutex factory not registered, mutex acquisition fails,
     *         or expression evaluation fails
     */
    @Expression(name = "syncRedis", description = "Synchronizes execution using a distributed Redis mutex")
    public static Object syncRedis(@Nullable String mutexName, @Nullable ISupplier<?> expression) {
        log.atTrace().log("Entering syncRedis(mutexName={}, expression={})", mutexName, expression);

        if (mutexName == null || mutexName.isBlank()) {
            throw new ExpressionException("syncRedis: mutex name cannot be null or blank");
        }

        if (expression == null) {
            throw new ExpressionException("syncRedis: expression cannot be null");
        }

        IMutexManager manager = MutexContext.get();
        if (manager == null) {
            throw new ExpressionException("syncRedis: no MutexManager available in context. " +
                    "Ensure MutexContext.set() is called during initialization with a manager " +
                    "that has RedisMutexFactory registered.");
        }

        try {
            // Create mutex name with Redis mutex type
            MutexName name = new MutexName(RedisMutex.class, mutexName);
            IMutex mutex = manager.mutex(name);

            log.atDebug().log("Acquiring Redis mutex: {}", name);

            // Execute expression inside mutex
            Object result = mutex.acquire(() -> {
                log.atTrace().log("Redis mutex acquired, evaluating expression");
                return expression.supply().orElse(null);
            });

            log.atDebug().log("Redis mutex released, result: {}", result);
            return result;

        } catch (MutexException e) {
            log.atError().log("syncRedis: mutex operation failed for '{}'", mutexName, e);
            throw new ExpressionException("syncRedis: mutex operation failed - " + e.getMessage());
        } catch (Exception e) {
            log.atError().log("syncRedis: expression evaluation failed", e);
            throw new ExpressionException("syncRedis: expression evaluation failed - " + e.getMessage());
        }
    }

    /**
     * Synchronizes execution using a Redis mutex with Object parameter.
     * This overload handles dynamic types from variable references.
     *
     * @param mutexName the name of the mutex (will be converted to String)
     * @param expression the expression (supplier) to execute while holding the mutex
     * @return the result of the expression evaluation
     * @throws ExpressionException if parameters are invalid or execution fails
     */
    @Expression(name = "syncRedis", description = "Synchronizes execution using a distributed Redis mutex")
    public static Object syncRedis(@Nullable Object mutexName, @Nullable ISupplier<?> expression) {
        String nameStr = mutexName == null ? null : mutexName.toString();
        return syncRedis(nameStr, expression);
    }
}
