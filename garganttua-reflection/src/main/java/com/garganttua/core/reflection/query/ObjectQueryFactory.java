package com.garganttua.core.reflection.query;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;

/**
 * Factory for {@link IObjectQuery} instances.
 */
public class ObjectQueryFactory {
    private static final Logger log = Logger.getLogger(ObjectQueryFactory.class);

    /**
     * Creates an {@link IObjectQuery} for the given class.
     *
     * @param <T>         the queried class type
     * @param objectClass the class whose members are queried
     * @param provider    the reflection provider used for resolution
     * @return a new object query
     * @throws ReflectionException if {@code objectClass} is null
     */
    public static <T> IObjectQuery<T> objectQuery(IClass<T> objectClass, IReflectionProvider provider) throws ReflectionException {
        log.debug("Creating ObjectQuery for class: {} with provider", objectClass);
        return new ObjectQuery<>(objectClass, provider);
    }

}
