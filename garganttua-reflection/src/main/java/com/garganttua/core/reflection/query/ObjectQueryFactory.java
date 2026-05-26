package com.garganttua.core.reflection.query;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;

public class ObjectQueryFactory {
    private static final IDiagnostic log = Diagnostics.of(ObjectQueryFactory.class);

    public static <T> IObjectQuery<T> objectQuery(IClass<T> objectClass, IReflectionProvider provider) throws ReflectionException {
        log.debug("Creating ObjectQuery for class: {} with provider", objectClass);
        return new ObjectQuery<>(objectClass, provider);
    }

}
