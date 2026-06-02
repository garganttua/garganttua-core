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

/**
 * Resolves {@code @Context}-annotated parameters to the current
 * {@link IRuntimeContext} supplier.
 *
 * <p>Registered for the {@link Context} annotation; the target parameter must be
 * assignable to {@link IRuntimeContext}.</p>
 */
@Resolver(annotations={Context.class})
public class ContextElementResolver implements IElementResolver {
    public ContextElementResolver() {
    }

    private static final Logger log = Logger.getLogger(ContextElementResolver.class);

    /**
     * Resolves the annotated element to a supplier of the current {@link IRuntimeContext}.
     *
     * @param elementType the declared type of the injection target; must be assignable to {@link IRuntimeContext}
     * @param element     the annotated element being resolved
     * @return a {@link Resolved} wrapping the runtime context supplier
     * @throws DiException if {@code elementType} is not an {@link IRuntimeContext}
     */
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