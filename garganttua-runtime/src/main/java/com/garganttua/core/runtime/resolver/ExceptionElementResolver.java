package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations={com.garganttua.core.runtime.annotations.Exception.class})
@NoArgsConstructor
public class ExceptionElementResolver implements IElementResolver {

    @SuppressWarnings("unchecked")
    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

        log.atTrace()
                .log("Resolving exception element");

        if (!Throwable.class.isAssignableFrom(elementType.getType())) {
            log.atError()
                    .log("Injectable is not a Throwable, throwing exception");
            throw new DiException("Injectable is not a Throwable: " + elementType.getSimpleName());
        }

        log.atDebug()
                .log("Element type is valid Throwable, preparing supplier");

        Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) elementType.getType();
        ISupplierBuilder<? extends Throwable, ?> s = exception(exceptionType);

        boolean nullable = isNullable(element);

        log.atDebug()
                .log("Resolved exception element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}