package com.garganttua.core.aot.commons;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.reflection.IClass;

/**
 * Central registry where AOT-generated class descriptors self-register.
 *
 * <p>Generated {@code AOTClass} implementations register themselves via a
 * {@code static} initializer block, making descriptors available for lookup
 * by the {@code AOTReflectionProvider} at runtime without classpath scanning.</p>
 */
public interface IAOTRegistry {

    /**
     * Registers an AOT-generated class descriptor.
     *
     * @param className the fully qualified class name
     * @param descriptor the pre-computed IClass descriptor
     * @param <T> the type represented by the descriptor
     */
    <T> void register(String className, IClass<T> descriptor);

    /**
     * Looks up a registered AOT descriptor by class name.
     *
     * @param className the fully qualified class name
     * @param <T> the expected type
     * @return the descriptor if registered, empty otherwise
     */
    <T> Optional<IClass<T>> get(String className);

    /**
     * Checks whether a descriptor exists for the given class name.
     *
     * @param className the fully qualified class name
     * @return true if a descriptor is registered
     */
    boolean contains(String className);

    /**
     * Returns the set of all registered class names.
     *
     * @return an unmodifiable view of registered class names
     */
    Set<String> registeredClasses();

}
