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
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Resolves {@code @Input} annotated step parameters to the runtime input supplier.
 *
 * @see Input
 * @see com.garganttua.core.runtime.RuntimeContext#input(IClass)
 */
@Resolver(annotations={Input.class})
public class InputElementResolver implements IElementResolver {
    /** Creates a resolver for {@code @Input} parameters. */
    public InputElementResolver() {
    }

    private static final Logger log = Logger.getLogger(InputElementResolver.class);

    /**
     * Builds a supplier that yields the runtime input value for the given element.
     *
     * @param elementType the declared type of the injectable element
     * @param element the annotated element being resolved
     * @return a successful {@link Resolved} wrapping the input supplier, honoring the element's nullability
     * @throws DiException if resolution fails
     */
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