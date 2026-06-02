package com.garganttua.core.injection.context.beans.resolver;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * {@link IElementResolver} that resolves injectable elements to beans created with the
 * {@link BeanStrategy#prototype} strategy (a fresh instance per injection).
 */
//@Resolver(annotations={Prototype.class})
public class PrototypeElementResolver extends BeanElementResolver implements IElementResolver {
    private static final Logger log = Logger.getLogger(PrototypeElementResolver.class);

    /**
     * Builds a prototype resolver aware of the given qualifier annotation types.
     *
     * @param qualifiers annotation types treated as bean qualifiers
     */
    public PrototypeElementResolver(Set<IClass<? extends Annotation>> qualifiers) {
        super(qualifiers);
        log.trace("Entering PrototypeElementResolver constructor with qualifiers: {}", qualifiers);
        log.debug("PrototypeElementResolver initialized with qualifiers: {}", qualifiers);
        log.trace("Exiting PrototypeElementResolver constructor");
    }

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) {
        log.trace("Entering resolve for elementType: {} and element: {}", elementType, element);

        Optional<ISupplierBuilder<?, ISupplier<?>>> builder = this.resolve(elementType, element,
                BeanStrategy.prototype);

        if (builder.isPresent()) {
            log.debug("Resolved prototype elementType {} with builder: {}", elementType, builder.get());
        } else {
            log.warn("Could not resolve prototype elementType: {}", elementType);
        }

        Resolved resolved = new Resolved(
                builder.isPresent(),
                elementType,
                builder.orElse(null),
                IInjectableElementResolver.isNullable(element));

        log.trace("Exiting resolve with resolved: {}", resolved);
        return resolved;
    }
}
