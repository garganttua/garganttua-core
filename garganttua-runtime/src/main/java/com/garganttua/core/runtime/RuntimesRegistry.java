package com.garganttua.core.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class RuntimesRegistry implements IBootstrapSummaryContributor, Map<String, IRuntime<?, ?>> {

    private final Map<String, IRuntime<?, ?>> runtimes;

    /**
     * Creates a new RuntimesRegistry with the given runtimes.
     *
     * @param runtimes the map of runtime name to runtime instance
     */
    public RuntimesRegistry(Map<String, IRuntime<?, ?>> runtimes) {
        Objects.requireNonNull(runtimes, "Runtimes map cannot be null");
        this.runtimes = Collections.unmodifiableMap(new LinkedHashMap<>(runtimes));
        log.atDebug().log("RuntimesRegistry created with {} runtimes", runtimes.size());
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

    @Override
    public String getSummaryCategory() {
        return "Runtime Engine";
    }

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

    @Override
    public int size() {
        return runtimes.size();
    }

    @Override
    public boolean isEmpty() {
        return runtimes.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return runtimes.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return runtimes.containsValue(value);
    }

    @Override
    public IRuntime<?, ?> get(Object key) {
        return runtimes.get(key);
    }

    @Override
    public IRuntime<?, ?> put(String key, IRuntime<?, ?> value) {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    @Override
    public IRuntime<?, ?> remove(Object key) {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    @Override
    public void putAll(Map<? extends String, ? extends IRuntime<?, ?>> m) {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("RuntimesRegistry is immutable");
    }

    @Override
    public Set<String> keySet() {
        return runtimes.keySet();
    }

    @Override
    public Collection<IRuntime<?, ?>> values() {
        return runtimes.values();
    }

    @Override
    public Set<Entry<String, IRuntime<?, ?>>> entrySet() {
        return runtimes.entrySet();
    }
}
