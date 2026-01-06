package com.garganttua.core.mutex;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe mutex manager implementation with configurable factory support.
 *
 * <p>
 * This implementation stores mutexes in a {@link ConcurrentHashMap}, where each
 * mutex is identified by a unique {@link MutexName}. The manager uses registered
 * {@link IMutexFactory} instances to create mutexes of specific types based on
 * the mutex type specified in the name.
 * </p>
 *
 * <h2>Factory Selection</h2>
 * <p>
 * When creating a mutex, the manager looks up the factory registered for the
 * mutex type. If no factory is found for the specific type, it defaults to
 * creating an {@link InterruptibleLeaseMutex}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The {@link ConcurrentHashMap} ensures thread-safe access to the mutex registry.
 * Individual mutex operations use the synchronization mechanisms provided by
 * their specific implementations.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Map<Class<? extends IMutex>, IMutexFactory> factories = new HashMap<>();
 * factories.put(InterruptibleLeaseMutex.class, new InterruptibleLeaseMutexFactory());
 *
 * IMutexManager manager = new MutexManager(factories);
 * MutexName name = MutexName.fromString("InterruptibleLeaseMutex::user:123");
 * IMutex userMutex = manager.mutex(name);
 * String result = userMutex.acquire(() -> {
 *     // Thread-safe critical section
 *     return updateUser("123");
 * });
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutexManager
 * @see IMutexFactory
 * @see MutexName
 */
@Slf4j
public class MutexManager implements IMutexManager {

    private final ConcurrentHashMap<String, IMutex> mutexes = new ConcurrentHashMap<>();
    private final Map<Class<? extends IMutex>, IMutexFactory> factories;

    /**
     * Constructs a new MutexManager with the specified factories.
     *
     * @param factories map of mutex types to their corresponding factories
     * @throws NullPointerException if factories is null
     */
    public MutexManager(Map<Class<? extends IMutex>, IMutexFactory> factories) {
        Objects.requireNonNull(factories, "Factories map cannot be null");
        this.factories = Collections.unmodifiableMap(new ConcurrentHashMap<>(factories));
        log.atInfo().log("MutexManager created with {} registered factories", factories.size());
    }

    /**
     * Constructs a new MutexManager with no factories (uses default factory).
     */
    public MutexManager() {
        this.factories = Collections.emptyMap();
        log.atInfo().log("MutexManager created with default factory");
    }

    @Override
    public IMutex mutex(MutexName name) throws MutexException {
        Objects.requireNonNull(name, "Mutex name cannot be null");

        String key = name.toString();

        return mutexes.computeIfAbsent(key, k -> {
            log.atDebug().log("Creating new mutex: {}", k);
            return createMutex(name);
        });
    }

    /**
     * Creates a new mutex using the appropriate factory based on the mutex type.
     *
     * @param name the mutex name containing type and identifier
     * @return a new mutex instance
     * @throws MutexException if mutex creation fails
     */
    private IMutex createMutex(MutexName name) throws MutexException {
        String type = name.type();
        String mutexName = name.name();

        // Try to find a factory by matching the type string with registered factory class names
        IMutexFactory factory = factories.values().stream()
                .filter(f -> {
                    Class<?>[] interfaces = f.getClass().getInterfaces();
                    for (Class<?> iface : interfaces) {
                        if (iface.getSimpleName().contains(type)) {
                            return true;
                        }
                    }
                    return f.getClass().getSimpleName().contains(type);
                })
                .findFirst()
                .orElse(null);

        if (factory != null) {
            log.atDebug().log("Using factory {} for mutex type {}", factory.getClass().getSimpleName(), type);
            return factory.createMutex(mutexName);
        }

        // Default fallback to InterruptibleLeaseMutex
        log.atWarn().log("No factory found for mutex type {}, using default InterruptibleLeaseMutex", type);
        return new InterruptibleLeaseMutex(mutexName);
    }

    @Override
    public Class<MutexName> getOwnerContextType() {
        return MutexName.class;
    }

    @Override
    public Type getSuppliedType() {
        return IMutex.class;
    }

}
