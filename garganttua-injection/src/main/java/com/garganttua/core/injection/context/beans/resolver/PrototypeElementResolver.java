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

public class PrototypeElementResolver extends BeanElementResolver
        implements IElementResolver {

    public PrototypeElementResolver(Set<Class<? extends Annotation>> qualifiers) {
        super(qualifiers);
    }

    @Override
    public Resolved resolve(Class<?> elementType,
            AnnotatedElement element) {

        Optional<IObjectSupplierBuilder<?,IObjectSupplier<?>>> builder = this.resolve(elementType, element, BeanStrategy.prototype);
        return new Resolved(builder.isPresent(),elementType, builder.orElse(null), IInjectableElementResolver.isNullable(element));
    }
}