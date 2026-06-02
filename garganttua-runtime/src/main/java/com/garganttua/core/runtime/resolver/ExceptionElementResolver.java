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
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Resolves {@code @Exception}-annotated parameters to a supplier of the caught
 * exception currently being handled by a fallback.
 *
 * <p>Registered for the runtime {@code @Exception} annotation; the target
 * parameter must be assignable to {@link Throwable}.</p>
 */
@Resolver(annotations={com.garganttua.core.runtime.annotations.Exception.class})
public class ExceptionElementResolver implements IElementResolver {
    public ExceptionElementResolver() {
    }

    private static final Logger log = Logger.getLogger(ExceptionElementResolver.class);

    /**
     * Resolves the annotated element to a supplier of the caught exception.
     *
     * @param elementType the declared type of the injection target; must be assignable to {@link Throwable}
     * @param element     the annotated element being resolved
     * @return a {@link Resolved} wrapping the exception supplier
     * @throws DiException if {@code elementType} is not a {@link Throwable}
     */
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