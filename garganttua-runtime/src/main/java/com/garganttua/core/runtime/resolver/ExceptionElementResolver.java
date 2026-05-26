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
import com.garganttua.core.supply.dsl.ISupplierBuilder;

@Resolver(annotations={com.garganttua.core.runtime.annotations.Exception.class})
public class ExceptionElementResolver implements IElementResolver {
    public ExceptionElementResolver() {
    }

    private static final IDiagnostic log = Diagnostics.of(ExceptionElementResolver.class);

    @SuppressWarnings("unchecked")
    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

        log.trace("Resolving exception element");

        if (!IClass.getClass(Throwable.class).isAssignableFrom(elementType)) {
            log.error("Injectable is not a Throwable, throwing exception");
            throw new DiException("Injectable is not a Throwable: " + elementType.getSimpleName());
        }

        log.debug("Element type is valid Throwable, preparing supplier");

        @SuppressWarnings("unchecked")
        IClass<? extends Throwable> exceptionType = (IClass<? extends Throwable>) (IClass<?>) elementType;
        ISupplierBuilder<? extends Throwable, ?> s = exception(exceptionType);

        boolean nullable = isNullable(element);

        log.debug("Resolved exception element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}