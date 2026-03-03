package com.garganttua.core.reflection.query;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectQueryFactory {

    public static <T> IObjectQuery<T> objectQuery(IClass<T> objectClass, IReflectionProvider provider) throws ReflectionException {
        log.atDebug().log("Creating ObjectQuery for class: {} with provider", objectClass);
        return new ObjectQuery<>(objectClass, provider);
    }

}
