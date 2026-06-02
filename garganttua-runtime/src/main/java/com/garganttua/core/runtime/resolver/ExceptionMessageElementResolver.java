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
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Resolves {@code @ExceptionMessage}-annotated parameters to a supplier of the
 * caught exception's message.
 *
 * <p>Registered for the {@link ExceptionMessage} annotation; the target
 * parameter must be assignable to {@link String}.</p>
 */
@Resolver(annotations={ExceptionMessage.class})
public class ExceptionMessageElementResolver implements IElementResolver {
    public ExceptionMessageElementResolver() {
    }

    private static final Logger log = Logger.getLogger(ExceptionMessageElementResolver.class);

    /**
     * Resolves the annotated element to a supplier of the caught exception's message.
     *
     * @param elementType the declared type of the injection target; must be assignable to {@link String}
     * @param element     the annotated element being resolved
     * @return a {@link Resolved} wrapping the exception-message supplier
     * @throws DiException if {@code elementType} is not a {@link String}
     */
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
