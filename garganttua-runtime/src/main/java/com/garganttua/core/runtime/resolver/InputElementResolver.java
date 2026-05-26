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
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.NoArgsConstructor;

@Resolver(annotations={Input.class})
@NoArgsConstructor
public class InputElementResolver implements IElementResolver {
    private static final IDiagnostic log = Diagnostics.of(InputElementResolver.class);

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

        log.trace("Resolving input element");

        log.debug("Preparing input supplier");

        ISupplierBuilder<?, ?> s = input(elementType);

        boolean nullable = isNullable(element);

        log.debug("Resolved input element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}