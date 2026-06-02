package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.annotations.Context;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

@Resolver(annotations={Context.class})
public class ContextElementResolver implements IElementResolver {
    public ContextElementResolver() {
    }

    private static final Logger log = Logger.getLogger(ContextElementResolver.class);

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

        log.trace("Resolving context element");

        if (!IClass.getClass(IRuntimeContext.class).isAssignableFrom(elementType)) {
            log.error("Injectable is not an IRuntimeContext, throwing exception");
            throw new DiException("Injectable is not a IRuntimeContext : " + elementType.getSimpleName());
        }

        log.debug("Element type is valid IRuntimeContext, preparing supplier");

        ISupplierBuilder<IRuntimeContext<Object, Object>, IContextualSupplier<IRuntimeContext<Object, Object>, IRuntimeContext<Object, Object>>> s = context();

        boolean nullable = isNullable(element);

        log.debug("Resolved context element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}