package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

@Resolver(annotations={Variable.class})
public class VariableElementResolver implements IElementResolver {
    public VariableElementResolver() {
    }

    private static final IDiagnostic log = Diagnostics.of(VariableElementResolver.class);

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

        log.trace("Resolving variable element");

        Variable annotation = element.getAnnotation(IClass.getClass(Variable.class));

        if (annotation == null) {
            log.error("Injectable is not annotated with @Variable, throwing exception");
            throw new DiException("Injectable is not annotated with @Variable");
        }

        String name = annotation.name();

        log.debug("Preparing variable supplier");

        ISupplierBuilder<?, ?> s = variable(name, elementType);

        boolean nullable = isNullable(element);

        log.debug("Resolved variable element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}