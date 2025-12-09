package com.garganttua.core.injection.context.beans.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations={Singleton.class, Inject.class})
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

        Optional<ISupplierBuilder<?, ISupplier<?>>> builder = this.resolve(elementType, element,
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