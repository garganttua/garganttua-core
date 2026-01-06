package com.garganttua.core.mutex;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe mutex manager implementation using synchronized Object locks.
 *
 * <p>
 * This implementation stores mutexes in a {@link ConcurrentHashMap}, where each
 * mutex is identified by a unique string name. Each mutex uses Java's intrinsic
 * {@code synchronized} mechanism on dedicated Object instances.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The {@link ConcurrentHashMap} ensures thread-safe access to the mutex registry.
 * Individual mutex operations use {@code synchronized} blocks for mutual exclusion.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IMutexManager manager = new SynchronizedMutexManager();
 * IMutex userMutex = manager.mutex("user:123");
 * String result = userMutex.acquire(() -> {
 *     // Thread-safe critical section
 *     return updateUser("123");
 * });
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutexManager
 * @see SynchronizedMutex
 */
@Slf4j
public class SynchronizedMutexManager implements IMutexManager {

    private final ConcurrentHashMap<String, IMutex> mutexes = new ConcurrentHashMap<>();

    /**
     * Constructs a new SynchronizedMutexManager.
     */
    public SynchronizedMutexManager() {
        log.atTrace().log("SynchronizedMutexManager created");
    }

    @Override
    public IMutex mutex(String name) throws MutexException {
        if (name == null || name.isEmpty()) {
            throw new MutexException("Mutex name cannot be null or empty");
        }

        return mutexes.computeIfAbsent(name, key -> {
            log.atDebug().log("Creating new mutex: {}", key);
            return new SynchronizedMutex(key);
        });
    }

    @Override
    public Class<String> getOwnerContextType() {
        return String.class;
    }

    @Override
    public Type getSuppliedType() {
        return IMutex.class;
    }

}
