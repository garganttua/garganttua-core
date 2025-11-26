package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        log.atTrace()
                .log("Resolving context element");

        if (!IRuntimeContext.class.isAssignableFrom(elementType)) {
            log.atError()
                    .log("Injectable is not an IRuntimeContext, throwing exception");
            throw new DiException("Injectable is not a IRuntimeContext : " + elementType.getSimpleName());
        }

        log.atDebug()
                .log("Element type is valid IRuntimeContext, preparing supplier");

        IObjectSupplierBuilder<IRuntimeContext<Object, Object>, IContextualObjectSupplier<IRuntimeContext<Object, Object>, IRuntimeContext<Object, Object>>> s = context();

        boolean nullable = isNullable(element);

        log.atInfo()
                .log("Resolved context element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}