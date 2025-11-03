package com.garganttua.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.IInjectableBuilderFactory;
import com.garganttua.core.injection.IInjectableBuilderRegistry;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class InjectableBuilderRegistry implements IInjectableBuilderRegistry {

    private Map<Class<? extends Annotation>, IInjectableBuilderFactory> factories = new HashMap<>();

    @Override
    public void registerFactory(Class<? extends Annotation> annotation, IInjectableBuilderFactory factory) {
        Objects.requireNonNull(annotation, "Annotation cannot be null");
        Objects.requireNonNull(factory, "Factory cannot be null");
        this.factories.put(annotation, factory);
    }

    @Override
    public Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> createBuilder(Class<?> elementType,
            AnnotatedElement element) {

        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            IInjectableBuilderFactory factory = this.factories.get(type);
            if (factory != null) {
                return factory.createBuilder(elementType, element);
            }
        }

        return Optional.empty();
    }

}
