package com.garganttua.core.mutex;

/**
 * Factory interface for creating mutex instances.
 *
 * <p>
 * {@code IMutexFactory} provides an abstraction for instantiating different
 * types of mutex implementations. This enables pluggable mutex strategies
 * (e.g., local synchronized mutexes, distributed locks, Redis-based mutexes)
 * without coupling the application to specific implementations.
 * </p>
 *
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li>Strategy pattern for mutex creation</li>
 *   <li>Dependency injection of mutex factories</li>
 *   <li>Testing with mock mutex implementations</li>
 *   <li>Switching between local and distributed locking mechanisms</li>
 * </ul>
 *
 * <h2>Implementation Examples</h2>
 * <ul>
 *   <li><b>LocalMutexFactory</b>: Creates JVM-local mutexes using ReentrantLock</li>
 *   <li><b>RedisMutexFactory</b>: Creates distributed mutexes using Redis</li>
 *   <li><b>DatabaseMutexFactory</b>: Creates mutexes using database row locks</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a factory
 * IMutexFactory factory = new SynchronizedMutexFactory();
 *
 * // Create a mutex instance
 * IMutex mutex = factory.createMutex("resource-lock");
 *
 * // Use the mutex
 * mutex.acquire(() -> {
 *     // Critical section code
 *     return result;
 * });
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutex
 * @see IMutexManager
 */
public interface IMutexFactory {

    /**
     * Creates a new mutex instance with the specified name.
     *
     * <p>
     * The name parameter serves as a unique identifier for the mutex.
     * Implementations may use this name for logging, monitoring, or
     * as a key in distributed locking systems.
     * </p>
     *
     * <p>
     * <b>Note:</b> This method creates a new mutex instance each time it is called.
     * For shared mutex management across the application, use {@link IMutexManager}
     * instead, which maintains a registry of mutexes by name.
     * </p>
     *
     * @param name the unique name identifying the mutex (must not be null or empty)
     * @return a new mutex instance
     * @throws MutexException if the mutex cannot be created (e.g., invalid name,
     *                        connection failure for distributed locks)
     * @throws IllegalArgumentException if name is null or empty
     */
    IMutex createMutex(String name) throws MutexException;

    /**
     * Creates a new mutex instance with the specified name and a predefined acquisition strategy.
     *
     * <p>
     * This method allows creating a mutex with default behavior configured through
     * a {@link MutexStrategy}. The strategy defines default values for:
     * </p>
     * <ul>
     *   <li>Wait timeout for lock acquisition</li>
     *   <li>Retry attempts and intervals</li>
     *   <li>Lease time for automatic lock release</li>
     * </ul>
     *
     * <p>
     * The returned mutex will use this strategy as its default when the single-parameter
     * {@link IMutex#acquire(IMutex.ThrowingFunction)} method is called. Users can still
     * override the strategy by calling {@link IMutex#acquire(IMutex.ThrowingFunction, MutexStrategy)}
     * with a different strategy.
     * </p>
     *
     * <h2>Use Cases</h2>
     * <ul>
     *   <li><b>Short-lived operations</b>: Create mutex with short lease time to prevent deadlocks</li>
     *   <li><b>High-contention resources</b>: Configure aggressive retry strategy</li>
     *   <li><b>Critical sections</b>: Define strict timeout policies</li>
     *   <li><b>Distributed locks</b>: Set appropriate timeouts for network latency</li>
     * </ul>
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * // Create a factory
     * IMutexFactory factory = new SynchronizedMutexFactory();
     *
     * // Define a strategy: 5s wait, 3 retries with 100ms interval, 10s lease
     * MutexStrategy strategy = new MutexStrategy(
     *     5, TimeUnit.SECONDS,
     *     3, 100, TimeUnit.MILLISECONDS,
     *     10, TimeUnit.SECONDS
     * );
     *
     * // Create mutex with predefined strategy
     * IMutex mutex = factory.createMutex("resource-lock", strategy);
     *
     * // This will use the predefined strategy
     * mutex.acquire(() -> {
     *     // Critical section code
     *     return result;
     * });
     * }</pre>
     *
     * @param name the unique name identifying the mutex (must not be null or empty)
     * @param defaultStrategy the default acquisition strategy to use for this mutex
     *                        (must not be null)
     * @return a new mutex instance with the predefined strategy
     * @throws MutexException if the mutex cannot be created
     * @throws IllegalArgumentException if name is null/empty or defaultStrategy is null
     */
    IMutex createMutex(String name, MutexStrategy defaultStrategy) throws MutexException;

}
