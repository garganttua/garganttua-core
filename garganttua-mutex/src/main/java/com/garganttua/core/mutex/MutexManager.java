package com.garganttua.core.mutex;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.reflection.IClass;

/**
 * Default {@link IMutexManager} that lazily creates and caches named mutexes,
 * delegating instantiation to per-type {@link IMutexFactory} implementations.
 *
 * <p>
 * Mutexes are cached by their {@link MutexName#toString()} key in a
 * {@link ConcurrentHashMap}, so repeated lookups for the same name return the
 * same lock instance. When no factory is registered for a requested mutex type,
 * a default {@link InterruptibleLeaseMutex} is created.
 * </p>
 *
 * <p>
 * This manager is thread-safe and also contributes a "Mutex Manager" section to
 * the bootstrap startup summary via {@link IBootstrapSummaryContributor}.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutexManager
 * @see IMutexFactory
 * @see InterruptibleLeaseMutex
 */
public class MutexManager implements IMutexManager, IBootstrapSummaryContributor {
    private static final Logger log = Logger.getLogger(MutexManager.class);

    private final ConcurrentHashMap<String, IMutex> mutexes = new ConcurrentHashMap<>();
    private final Map<IClass<? extends IMutex>, IMutexFactory> factories;

    /**
     * Creates a manager backed by the given mutex-type to factory mappings.
     *
     * @param factories the factories keyed by the mutex type they produce; copied defensively and made immutable
     * @throws NullPointerException if {@code factories} is null
     */
    public MutexManager(Map<IClass<? extends IMutex>, IMutexFactory> factories) {
        Objects.requireNonNull(factories, "Factories map cannot be null");
        this.factories = Collections.unmodifiableMap(new ConcurrentHashMap<>(factories));
        log.debug("MutexManager created with {} registered factories", factories.size());
    }

    /**
     * Creates a manager with no registered factories; every mutex will fall back
     * to the default {@link InterruptibleLeaseMutex}.
     */
    public MutexManager() {
        this.factories = Collections.emptyMap();
        log.debug("MutexManager created with default factory");
    }

    /**
     * Returns the cached mutex for the given name, creating it on first access.
     *
     * @param name the mutex identity (type and name)
     * @return the shared {@link IMutex} instance for {@code name}
     * @throws MutexException if mutex creation fails
     * @throws NullPointerException if {@code name} is null
     */
    @Override
    public IMutex mutex(MutexName name) throws MutexException {
        Objects.requireNonNull(name, "Mutex name cannot be null");

        String key = name.toString();

        return mutexes.computeIfAbsent(key, k -> {
            log.debug("Creating new mutex: {}", k);
            return createMutex(name);
        });
    }

    private IMutex createMutex(MutexName name) throws MutexException {
        IClass<? extends IMutex> type = name.type();
        String mutexName = name.name();

        IMutexFactory factory = factories.get(type);

        if (factory != null) {
            log.debug("Using factory {} for mutex type {}",
                    factory.getClass().getSimpleName(), type.getSimpleName());
            return factory.createMutex(mutexName);
        }

        log.warn("No factory found for mutex type {}, using default InterruptibleLeaseMutex",
                type.getSimpleName());
        return new InterruptibleLeaseMutex(mutexName);
    }

    @Override
    public IClass<MutexName> getOwnerContextType() {
        return IClass.getClass(MutexName.class);
    }

    @Override
    public Type getSuppliedType() {
        return IClass.getClass(IMutex.class).getType();
    }

    @Override
    public IClass<IMutex> getSuppliedClass() {
        return IClass.getClass(IMutex.class);
    }

    // --- IBootstrapSummaryContributor implementation ---

    /**
     * {@inheritDoc}
     *
     * @return the {@code "Mutex Manager"} category label
     */
    @Override
    public String getSummaryCategory() {
        return "Mutex Manager";
    }

    /**
     * {@inheritDoc}
     *
     * @return the registered factory count and active mutex count
     */
    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Mutex factories", String.valueOf(factories.size()));
        items.put("Active mutexes", String.valueOf(mutexes.size()));
        return items;
    }
}
