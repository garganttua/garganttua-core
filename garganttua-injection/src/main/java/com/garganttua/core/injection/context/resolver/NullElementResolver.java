package com.garganttua.core.injection.context.resolver;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

/**
 * {@link IElementResolver} for elements annotated with {@link Null}, supplying a {@code null}
 * value of the element's type.
 */
@Resolver(annotations={Null.class})
public class NullElementResolver implements IElementResolver {
    /** Creates a resolver for {@link Null}-annotated elements. */
    public NullElementResolver() {
    }

    private static final Logger log = Logger.getLogger(NullElementResolver.class);

    /**
     * Resolves the element to a supplier yielding {@code null} of the element's type.
     *
     * @return a resolved supplier producing {@code null}, carrying the element's nullability flag
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) {
        log.trace("Entering resolve with elementType: {} and element: {}", elementType, element);

        Objects.requireNonNull(element, "Element cannot be null");
        log.debug("Element is not null: {}", element);

        Objects.requireNonNull(elementType, "ElementType cannot be null");
        log.debug("ElementType is not null: {}", elementType);

        ISupplierBuilder<?, ISupplier<?>> builder = new NullSupplierBuilder(elementType);
        log.debug("Created NullSupplierBuilder for elementType: {}", elementType.getSimpleName());

        boolean nullable = IInjectableElementResolver.isNullable(element);
        log.debug("Element {} nullable: {}", element, nullable);

        Resolved resolved = new Resolved(true, elementType, builder, nullable);
        log.trace("Exiting resolve with Resolved: {}", resolved);

        return resolved;
    }
}
