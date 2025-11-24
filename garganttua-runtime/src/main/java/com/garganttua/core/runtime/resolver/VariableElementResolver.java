package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VariableElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        log.atTrace()
                .log("Resolving variable element");

        Variable annotation = element.getAnnotation(Variable.class);

        if (annotation == null) {
            log.atError().log("Injectable is not annotated with @Variable, throwing exception");
            throw new DiException("Injectable is not annotated with @Variable");
        }

        String name = annotation.name();

        log.atDebug()
                .log("Preparing variable supplier");

        IObjectSupplierBuilder<?, ?> s = variable(name, elementType);

        boolean nullable = isNullable(element);

        log.atInfo()
                .log("Resolved variable element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}