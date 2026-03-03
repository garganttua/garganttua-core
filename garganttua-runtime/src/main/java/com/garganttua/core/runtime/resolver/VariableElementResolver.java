package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations={Variable.class})
@NoArgsConstructor
public class VariableElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

        log.atTrace()
                .log("Resolving variable element");

        Variable annotation = element.getAnnotation(IClass.getClass(Variable.class));

        if (annotation == null) {
            log.atError().log("Injectable is not annotated with @Variable, throwing exception");
            throw new DiException("Injectable is not annotated with @Variable");
        }

        String name = annotation.name();

        log.atDebug()
                .log("Preparing variable supplier");

        ISupplierBuilder<?, ?> s = variable(name, elementType);

        boolean nullable = isNullable(element);

        log.atDebug()
                .log("Resolved variable element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}