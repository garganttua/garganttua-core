package com.garganttua.core.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;

/**
 * Registry that holds all built runtimes and provides summary information.
 *
 * <p>
 * This class wraps the map of runtimes built by {@code RuntimesBuilder} and
 * implements {@link IBootstrapSummaryContributor} to provide runtime statistics
 * in the bootstrap summary. It also implements {@link Map} for backward compatibility.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class RuntimesRegistry implements IBootstrapSummaryContributor, Map<String, IRuntime<?, ?>> {
    private static final Logger log = Logger.getLogger(RuntimesRegistry.class);

    private final Map<String, IRuntime<?, ?>> runtimes;

    /**
     * Creates a new RuntimesRegistry with the given runtimes.
     *
     * @param runtimes the map of runtime name to runtime instance
     */
    public RuntimesRegistry(Map<String, IRuntime<?, ?>> runtimes) {
        Objects.requireNonNull(runtimes, "Runtimes map cannot be null");
        this.runtimes = Collections.unmodifiableMap(new LinkedHashMap<>(runtimes));
        log.debug("RuntimesRegistry created with {} runtimes", runtimes.size());
    }

    /**
     * Gets a runtime by name with type casting.
     *
     * @param name the runtime name
     * @return an Optional containing the runtime if found
     */
    @SuppressWarnings("unchecked")
    public <I, O> Optional<IRuntime<I, O>> getRuntime(String name) {
        return Optional.ofNullable((IRuntime<I, O>) runtimes.get(name));
    }

    /**
     * Gets all runtimes.
     *
     * @return an unmodifiable map of all runtimes
     */
    public Map<String, IRuntime<?, ?>> getAll() {
        return runtimes;
    }

    // --- IBootstrapSummaryContributor implementation ---

    /**
     * Returns the bootstrap summary category label for registered runtimes.
     *
     * @return the summary category name
     */
    @Override
    public String getSummaryCategory() {
        return "Runtime Engine";
    }

    /**
     * Returns the bootstrap summary items describing the registered runtimes,
     * including the count and a (possibly truncated) list of runtime names.
     *
     * @return an ordered map of summary label to value
     */
    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Runtimes registered", String.valueOf(runtimes.size()));

        // List runtime names if there are any
        if (!runtimes.isEmpty()) {
            String names = String.join(", ", runtimes.keySet());
            if (names.length() > 50) {
                names = names.substring(0, 47) + "...";
            }
            items.put("Runtime names", names);
        }

        return items;
    }

    // --- Map interface implementation (delegation) ---

    /** {@inheritDoc} */
    @Override
    public int size() {
        return runtimes.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return runtimes.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsKey(Object key) {
        return runtimes.containsKey(key);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsValue(Object value) {
        return runtimes.containsValue(value);
    }

    /** {@inheritDoc} */
    @Override
    public IRuntime<?, ?> get(Object key) {
        return runtimes.get(key);
    }

    /**
     * Unsupported: this registry is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public IRuntime<?, ?> put(String key, IRuntime<?, ?> value) {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    /**
     * Unsupported: this registry is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public IRuntime<?, ?> remove(Object key) {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    /**
     * Unsupported: this registry is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void putAll(Map<? extends String, ? extends IRuntime<?, ?>> m) {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    /**
     * Unsupported: this registry is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> keySet() {
        return runtimes.keySet();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<IRuntime<?, ?>> values() {
        return runtimes.values();
    }

    /** {@inheritDoc} */
    @Override
    public Set<Entry<String, IRuntime<?, ?>>> entrySet() {
        return runtimes.entrySet();
    }
}
