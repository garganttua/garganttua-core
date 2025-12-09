package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations={ExceptionMessage.class})
public class ExceptionMessageElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        log.atTrace()
                .log("Resolving exception message element");

        if (!String.class.isAssignableFrom(elementType)) {
            log.atError()
                    .log("Injectable is not a String, throwing exception");
            throw new DiException("Injectable is not a String: " + elementType.getSimpleName());
        }

        log.atDebug()
                .log("Element type is valid String, preparing supplier");

        ISupplierBuilder<String, IContextualSupplier<String, IRuntimeContext<Object, Object>>> s = exceptionMessage();

        boolean nullable = isNullable(element);

        log.atInfo()
                .addKeyValue("nullable", nullable)
                .log("Resolved exception message element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}
