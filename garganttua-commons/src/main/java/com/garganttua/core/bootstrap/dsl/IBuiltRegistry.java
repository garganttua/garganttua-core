package com.garganttua.core.bootstrap.dsl;

import java.util.List;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

/**
 * Read-only registry of the objects produced by a {@code Bootstrap} build,
 * keyed by their type for lookup by downstream consumers.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IBuiltRegistry {

    /**
     * Looks up the first built object assignable to the given type.
     *
     * @param <T>   the requested type
     * @param clazz the type to match
     * @return an {@link Optional} holding the matching built object, or empty if none matches
     */
    <T>Optional<T> request(IClass<T> clazz);

    /**
     * @return the number of built objects held in this registry
     */
    Integer size();

    /**
     * @return an immutable snapshot of every built object, in build order
     */
    List<Object> toList();

}
