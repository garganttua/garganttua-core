package com.garganttua.core.mutex;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MutexManager implements IMutexManager, IBootstrapSummaryContributor {

    private final ConcurrentHashMap<String, IMutex> mutexes = new ConcurrentHashMap<>();
    private final Map<IClass<? extends IMutex>, IMutexFactory> factories;

    public MutexManager(Map<IClass<? extends IMutex>, IMutexFactory> factories) {
        Objects.requireNonNull(factories, "Factories map cannot be null");
        this.factories = Collections.unmodifiableMap(new ConcurrentHashMap<>(factories));
        log.atDebug().log("MutexManager created with {} registered factories", factories.size());
    }

    public MutexManager() {
        this.factories = Collections.emptyMap();
        log.atDebug().log("MutexManager created with default factory");
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

    private IMutex createMutex(MutexName name) throws MutexException {
        IClass<? extends IMutex> type = name.type();
        String mutexName = name.name();

        IMutexFactory factory = factories.get(type);

        if (factory != null) {
            log.atDebug().log("Using factory {} for mutex type {}",
                    factory.getClass().getSimpleName(), type.getSimpleName());
            return factory.createMutex(mutexName);
        }

        log.atWarn().log("No factory found for mutex type {}, using default InterruptibleLeaseMutex",
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

    @Override
    public String getSummaryCategory() {
        return "Mutex Manager";
    }

    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Mutex factories", String.valueOf(factories.size()));
        items.put("Active mutexes", String.valueOf(mutexes.size()));
        return items;
    }
}
