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
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.NoArgsConstructor;

@Resolver(annotations={ExceptionMessage.class})
@NoArgsConstructor
public class ExceptionMessageElementResolver implements IElementResolver {
    private static final IDiagnostic log = Diagnostics.of(ExceptionMessageElementResolver.class);

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

        log.trace("Resolving exception message element");

        if (!IClass.getClass(String.class).isAssignableFrom(elementType)) {
            log.error("Injectable is not a String, throwing exception");
            throw new DiException("Injectable is not a String: " + elementType.getSimpleName());
        }

        log.debug("Element type is valid String, preparing supplier");

        ISupplierBuilder<String, IContextualSupplier<String, IRuntimeContext<Object, Object>>> s = exceptionMessage();

        boolean nullable = isNullable(element);

        log.debug("Resolved exception message element successfully (nullable={})", nullable);

        return new Resolved(true, elementType, s, nullable);
    }
}
