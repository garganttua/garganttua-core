package com.garganttua.core.aot.commons;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.reflection.IClass;

/**
 * Thread-safe singleton implementation of {@link IAOTRegistry}.
 *
 * <p>AOT-generated classes call {@code AOTRegistry.getInstance().register(...)}
 * in their static initializer blocks. The registry uses a {@link ConcurrentHashMap}
 * for lock-free reads and safe concurrent writes during class loading.</p>
 */
public final class AOTRegistry implements IAOTRegistry {

    private static final AOTRegistry INSTANCE = new AOTRegistry();

    private final ConcurrentHashMap<String, IClass<?>> descriptors = new ConcurrentHashMap<>();

    private AOTRegistry() {
    }

    public static AOTRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public <T> void register(String className, IClass<T> descriptor) {
        if (className == null || descriptor == null) {
            throw new IllegalArgumentException("className and descriptor must not be null");
        }
        descriptors.put(className, descriptor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<IClass<T>> get(String className) {
        return Optional.ofNullable((IClass<T>) descriptors.get(className));
    }

    @Override
    public boolean contains(String className) {
        return descriptors.containsKey(className);
    }

    @Override
    public Set<String> registeredClasses() {
        return Collections.unmodifiableSet(descriptors.keySet());
    }

}
