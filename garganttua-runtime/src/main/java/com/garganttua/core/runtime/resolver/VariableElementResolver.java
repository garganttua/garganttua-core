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
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Resolves {@code @Variable} annotated step parameters to a named runtime variable supplier.
 *
 * @see Variable
 * @see com.garganttua.core.runtime.RuntimeContext#variable(String, IClass)
 */
@Resolver(annotations={Variable.class})
public class VariableElementResolver implements IElementResolver {
    /** Creates a resolver for {@code @Variable} parameters. */
    public VariableElementResolver() {
    }

    private static final Logger log = Logger.getLogger(VariableElementResolver.class);

    /**
     * Builds a supplier that yields the runtime variable named by the {@link Variable} annotation.
     *
     * @param elementType the declared type of the injectable element
     * @param element the annotated element being resolved
     * @return a successful {@link Resolved} wrapping the variable supplier, honoring the element's nullability
     * @throws DiException if the element is not annotated with {@link Variable}, or if resolution otherwise fails
     */
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