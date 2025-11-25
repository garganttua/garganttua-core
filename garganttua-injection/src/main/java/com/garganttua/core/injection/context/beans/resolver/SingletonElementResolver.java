package com.garganttua.core.injection.context.beans.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SingletonElementResolver extends BeanElementResolver implements IElementResolver {

    public SingletonElementResolver(Set<Class<? extends Annotation>> qualifiers) {
        super(qualifiers);
        log.atTrace().log("Entering SingletonElementResolver constructor with qualifiers: {}", qualifiers);
        log.atInfo().log("SingletonElementResolver initialized with qualifiers: {}", qualifiers);
        log.atTrace().log("Exiting SingletonElementResolver constructor");
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) {
        log.atTrace().log("Entering resolve for elementType: {} and element: {}", elementType, element);

        Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.resolve(elementType, element,
                BeanStrategy.singleton);

        if (builder.isPresent()) {
            log.atInfo().log("Resolved singleton elementType {} with builder: {}", elementType, builder.get());
        } else {
            log.atWarn().log("Could not resolve singleton elementType: {}", elementType);
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