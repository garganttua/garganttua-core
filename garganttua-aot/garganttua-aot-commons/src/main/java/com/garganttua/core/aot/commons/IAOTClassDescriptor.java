package com.garganttua.core.aot.commons;

import com.garganttua.core.reflection.IClass;

/**
 * Marker interface for AOT-generated {@link IClass} implementations.
 *
 * <p>AOT class descriptors contain pre-computed metadata (fields, methods,
 * constructors, annotations) populated at compile time. Structural queries
 * on an {@code IAOTClassDescriptor} read final fields — no runtime
 * introspection is performed.</p>
 *
 * @param <T> the type represented by this descriptor
 */
public interface IAOTClassDescriptor<T> extends IClass<T> {

    /**
     * Always returns {@code true} for AOT-generated descriptors.
     *
     * @return true
     */
    default boolean isAOTGenerated() {
        return true;
    }

}
