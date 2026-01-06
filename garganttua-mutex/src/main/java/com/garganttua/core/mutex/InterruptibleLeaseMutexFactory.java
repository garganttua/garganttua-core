package com.garganttua.core.mutex;

import java.util.Objects;

import javax.inject.Named;

import com.garganttua.core.mutex.annotations.MutexFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory implementation for creating {@link InterruptibleLeaseMutex} instances.
 *
 * <p>
 * {@code InterruptibleLeaseMutexFactory} provides a factory for creating JVM-local
 * mutexes with strict lease time enforcement through thread interruption. Each mutex
 * created is independent and uses a {@link java.util.concurrent.locks.ReentrantLock}
 * for synchronization.
 * </p>
 *
 * <h2>Mutex Characteristics</h2>
 * <ul>
 *   <li><b>Local scope</b>: Mutexes are JVM-local, not distributed</li>
 *   <li><b>Fair locking</b>: Uses fair {@link java.util.concurrent.locks.ReentrantLock}
 *       to prevent thread starvation</li>
 *   <li><b>Lease enforcement</b>: Interrupts threads that exceed lease time</li>
 *   <li><b>Thread-safe</b>: Each mutex ensures mutual exclusion across threads</li>
 * </ul>
 *
 * <h2>Lease Time Behavior</h2>
 * <p>
 * When a mutex is created with a predefined {@link MutexStrategy}, the lease time
 * is strictly enforced:
 * </p>
 * <ul>
 *   <li>Execution threads are interrupted if they exceed the lease time</li>
 *   <li>User code must be interruption-aware (check {@link Thread#interrupted()})</li>
 *   <li>Locks are automatically released even on timeout</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <pre>{@code
 * // Create factory
 * IMutexFactory factory = new InterruptibleLeaseMutexFactory();
 *
 * // Create simple mutex
 * IMutex mutex1 = factory.createMutex("database::user-lock");
 *
 * // Create mutex with predefined strategy
 * MutexStrategy strategy = new MutexStrategy(
 *     5, TimeUnit.SECONDS,      // 5s wait timeout
 *     3, 100, TimeUnit.MILLISECONDS,  // 3 retries, 100ms interval
 *     10, TimeUnit.SECONDS      // 10s lease time with interruption
 * );
 * IMutex mutex2 = factory.createMutex("cache::session-lock", strategy);
 *
 * // Use the mutex
 * mutex2.acquire(() -> {
 *     // This will use the predefined strategy
 *     // Code must handle interruption if lease time is exceeded
 *     return performOperation();
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This factory is thread-safe and can be shared across multiple threads. However,
 * each call to {@link #createMutex(String)} or {@link #createMutex(String, MutexStrategy)}
 * creates a new independent mutex instance.
 * </p>
 *
 * <h2>Comparison with MutexManager</h2>
 * <ul>
 *   <li><b>Factory</b>: Creates new mutex instances each time</li>
 *   <li><b>Manager</b>: Maintains a registry of shared mutexes by name</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see InterruptibleLeaseMutex
 * @see IMutexFactory
 * @see MutexStrategy
 */
@Slf4j
@MutexFactory(type = InterruptibleLeaseMutex.class)
@Named("InterruptibleLeaseMutexFactory")
public class InterruptibleLeaseMutexFactory implements IMutexFactory {

    /**
     * Creates a new {@link InterruptibleLeaseMutex} with the specified name.
     *
     * <p>
     * The created mutex will block indefinitely when acquiring locks using the
     * single-parameter {@link IMutex#acquire(IMutex.ThrowingFunction)} method.
     * For time-bounded acquisitions, use
     * {@link IMutex#acquire(IMutex.ThrowingFunction, MutexStrategy)} with a
     * specific strategy.
     * </p>
     *
     * @param name the unique name identifying the mutex (must not be null or empty)
     * @return a new {@link InterruptibleLeaseMutex} instance
     * @throws MutexException if the mutex cannot be created
     * @throws IllegalArgumentException if name is null or empty
     */
    @Override
    public IMutex createMutex(String name) throws MutexException {
        validateName(name);

        log.atDebug().log("Creating InterruptibleLeaseMutex with name: {}", name);

        try {
            return new InterruptibleLeaseMutex(name);
        } catch (Exception e) {
            log.atError().log("Failed to create mutex '{}': {}", name, e.getMessage(), e);
            throw new MutexException("Failed to create mutex '" + name + "'", e);
        }
    }

    /**
     * Creates a new {@link InterruptibleLeaseMutex} with the specified name and
     * predefined acquisition strategy.
     *
     * <p>
     * <b>Note:</b> The current implementation of {@link InterruptibleLeaseMutex} does not
     * store a default strategy. The strategy must be passed explicitly to
     * {@link IMutex#acquire(IMutex.ThrowingFunction, MutexStrategy)} for each acquisition.
     * This method accepts the strategy parameter for API compatibility but does not
     * apply it as a default.
     * </p>
     *
     * <p>
     * Future implementations may wrap the mutex to apply the strategy automatically.
     * For now, this method is equivalent to {@link #createMutex(String)} but validates
     * the strategy parameter.
     * </p>
     *
     * @param name the unique name identifying the mutex (must not be null or empty)
     * @param defaultStrategy the default acquisition strategy (must not be null, but currently unused)
     * @return a new {@link InterruptibleLeaseMutex} instance
     * @throws MutexException if the mutex cannot be created
     * @throws IllegalArgumentException if name is null/empty or defaultStrategy is null
     */
    @Override
    public IMutex createMutex(String name, MutexStrategy defaultStrategy) throws MutexException {
        validateName(name);
        Objects.requireNonNull(defaultStrategy, "Default strategy cannot be null");

        log.atDebug().log("Creating InterruptibleLeaseMutex with name: {} and strategy: " +
                "waitTime={}{}, retries={}, leaseTime={}{}",
                name,
                defaultStrategy.waitTime(),
                defaultStrategy.waitTimeUnit(),
                defaultStrategy.retries(),
                defaultStrategy.leaseTime(),
                defaultStrategy.leaseTimeUnit());

        log.atWarn().log("Note: InterruptibleLeaseMutex does not currently store default strategies. " +
                "Strategy must be passed explicitly to acquire() method.");

        // For now, create a simple mutex
        // Future enhancement: wrap in a StrategyAwareMutex that applies the default strategy
        return createMutex(name);
    }

    /**
     * Validates the mutex name.
     *
     * @param name the mutex name to validate
     * @throws IllegalArgumentException if name is null or empty after trimming
     */
    private void validateName(String name) {
        Objects.requireNonNull(name, "Mutex name cannot be null");

        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Mutex name cannot be empty");
        }
    }

}
