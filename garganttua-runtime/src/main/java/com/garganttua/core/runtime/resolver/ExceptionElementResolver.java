package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionElementResolver implements IElementResolver {

    @SuppressWarnings("unchecked")
    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        log.atTrace()
                .log("Resolving exception element");

        if (!Throwable.class.isAssignableFrom(elementType)) {
            log.atError()
                    .log("Injectable is not a Throwable, throwing exception");
            throw new DiException("Injectable is not a Throwable: " + elementType.getSimpleName());
        }

        log.atDebug()
                .log("Element type is valid Throwable, preparing supplier");

        Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) elementType;
        IObjectSupplierBuilder<? extends Throwable, ?> s = exception(exceptionType);

        boolean nullable = isNullable(element);

        log.atInfo()
                .log("Resolved exception element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}