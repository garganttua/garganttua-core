package com.garganttua.core.injection.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InjectableElementResolver implements IInjectableElementResolver {

    private Map<Class<? extends Annotation>, IElementResolver> resolvers = new HashMap<>();

    @Override
    public Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> resolve(Class<?> elementType,
            AnnotatedElement element) {

        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            IElementResolver resolver = this.resolvers.get(type);
            if (resolver != null) {
                return resolver.resolve(elementType, element);
            }
        }

        return Optional.empty();
    }

}
