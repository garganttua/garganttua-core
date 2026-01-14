package com.garganttua.core.mutex.redis;

import java.util.Objects;

import javax.inject.Named;

import org.github.siahsang.redutils.common.RedUtilsConfig;

import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;
import com.garganttua.core.mutex.annotations.MutexFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory implementation for creating {@link RedisMutex} instances.
 *
 * <p>
 * {@code RedisMutexFactory} provides a factory for creating distributed
 * mutexes backed by Redis. Each mutex created is independent and can
 * coordinate lock acquisition across multiple JVMs and processes.
 * </p>
 *
 * <h2>Mutex Characteristics</h2>
 * <ul>
 *   <li><b>Distributed scope</b>: Mutexes work across multiple JVMs/processes</li>
 *   <li><b>Redis-backed</b>: Uses Redis as the coordination service</li>
 *   <li><b>Configurable</b>: Supports custom Redis configuration</li>
 *   <li><b>Thread-safe</b>: Each mutex ensures mutual exclusion across distributed systems</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>
 * The factory can be configured with custom Redis settings via {@link RedUtilsConfig}.
 * If no configuration is provided, the default Redis connection (localhost:6379) is used.
 * </p>
 *
 * <h2>Usage Patterns</h2>
 * <pre>{@code
 * // Create factory with default Redis configuration
 * IMutexFactory factory = new RedisMutexFactory();
 *
 * // Create simple mutex
 * IMutex mutex1 = factory.createMutex("distributed::user-lock");
 *
 * // Create factory with custom configuration
 * RedUtilsConfig config = new RedUtilsConfig.RedUtilsConfigBuilder()
 *     .hostAddress("redis.example.com")
 *     .port("6379")
 *     .replicaCount(3)
 *     .leaseTimeMillis(40_000)
 *     .build();
 * IMutexFactory customFactory = new RedisMutexFactory(config);
 *
 * // Create mutex with predefined strategy
 * MutexStrategy strategy = new MutexStrategy(
 *     5, TimeUnit.SECONDS,      // 5s wait timeout
 *     3, 100, TimeUnit.MILLISECONDS,  // 3 retries, 100ms interval
 *     10, TimeUnit.SECONDS      // 10s lease time
 * );
 * IMutex mutex2 = factory.createMutex("cache::session-lock", strategy);
 *
 * // Use the mutex
 * mutex2.acquire(() -> {
 *     // Critical section code across distributed systems
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
 * @since 2.0.0-ALPHA01
 * @see RedisMutex
 * @see IMutexFactory
 * @see MutexStrategy
 */
@Slf4j
@MutexFactory(type = RedisMutex.class)
@Named("RedisMutexFactory")
public class RedisMutexFactory implements IMutexFactory {

    private final RedUtilsConfig redisConfig;

    /**
     * Creates a factory with default Redis configuration (localhost:6379).
     */
    public RedisMutexFactory() {
        this.redisConfig = null;
        log.atDebug().log("Created RedisMutexFactory with default configuration");
    }

    /**
     * Creates a factory with custom Redis configuration.
     *
     * @param redisConfig the Redis configuration to use for all created mutexes
     */
    public RedisMutexFactory(RedUtilsConfig redisConfig) {
        this.redisConfig = Objects.requireNonNull(redisConfig, "Redis configuration cannot be null");
        log.atDebug().log("Created RedisMutexFactory with custom configuration");
    }

    /**
     * Creates a new {@link RedisMutex} with the specified name.
     *
     * <p>
     * The created mutex will use the Redis configuration provided to this factory
     * (or default configuration if none was specified). The mutex will block
     * indefinitely when acquiring locks using the single-parameter
     * {@link IMutex#acquire(IMutex.ThrowingFunction)} method.
     * </p>
     *
     * @param name the unique name identifying the mutex (must not be null or empty)
     * @return a new {@link RedisMutex} instance
     * @throws MutexException if the mutex cannot be created
     * @throws IllegalArgumentException if name is null or empty
     */
    @Override
    public IMutex createMutex(String name) throws MutexException {
        validateName(name);

        log.atDebug().log("Creating RedisMutex with name: {}", name);

        try {
            if (redisConfig != null) {
                return new RedisMutex(name, redisConfig);
            } else {
                return new RedisMutex(name);
            }
        } catch (Exception e) {
            log.atError().log("Failed to create Redis mutex '{}': {}", name, e.getMessage(), e);
            throw new MutexException("Failed to create Redis mutex '" + name + "'", e);
        }
    }

    /**
     * Creates a new {@link RedisMutex} with the specified name and
     * predefined acquisition strategy.
     *
     * <p>
     * <b>Note:</b> The current implementation of {@link RedisMutex} does not
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
     * @return a new {@link RedisMutex} instance
     * @throws MutexException if the mutex cannot be created
     * @throws IllegalArgumentException if name is null/empty or defaultStrategy is null
     */
    @Override
    public IMutex createMutex(String name, MutexStrategy defaultStrategy) throws MutexException {
        validateName(name);
        Objects.requireNonNull(defaultStrategy, "Default strategy cannot be null");

        log.atDebug().log("Creating RedisMutex with name: {} and strategy: " +
                "waitTime={}{}, retries={}, leaseTime={}{}",
                name,
                defaultStrategy.waitTime(),
                defaultStrategy.waitTimeUnit(),
                defaultStrategy.retries(),
                defaultStrategy.leaseTime(),
                defaultStrategy.leaseTimeUnit());

        log.atWarn().log("Note: RedisMutex does not currently store default strategies. " +
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
