package com.garganttua.core.injection.context.beans.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Resolver(annotations={Prototype.class})
public class PrototypeElementResolver extends BeanElementResolver implements IElementResolver {

    public PrototypeElementResolver(Set<Class<? extends Annotation>> qualifiers) {
        super(qualifiers);
        log.atTrace().log("Entering PrototypeElementResolver constructor with qualifiers: {}", qualifiers);
        log.atDebug().log("PrototypeElementResolver initialized with qualifiers: {}", qualifiers);
        log.atTrace().log("Exiting PrototypeElementResolver constructor");
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) {
        log.atTrace().log("Entering resolve for elementType: {} and element: {}", elementType, element);

        Optional<ISupplierBuilder<?, ISupplier<?>>> builder = this.resolve(elementType, element,
                BeanStrategy.prototype);

        if (builder.isPresent()) {
            log.atDebug().log("Resolved prototype elementType {} with builder: {}", elementType, builder.get());
        } else {
            log.atWarn().log("Could not resolve prototype elementType: {}", elementType);
        }

        Resolved resolved = new Resolved(
                builder.isPresent(),
                elementType,
                builder.orElse(null),
                IInjectableElementResolver.isNullable(element));

        log.atTrace().log("Exiting resolve with resolved: {}", resolved);
        return resolved;
    }
}