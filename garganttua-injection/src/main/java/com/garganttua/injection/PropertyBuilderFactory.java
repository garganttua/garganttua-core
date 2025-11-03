package com.garganttua.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.IInjectableBuilderFactory;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class PropertyBuilderFactory implements IInjectableBuilderFactory {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> createBuilder(Class<?> elementType,
            AnnotatedElement element) {
        Objects.requireNonNull(element, "Element cannot be null");
        Objects.requireNonNull(elementType, "ElementType cannot be null");

        String provider = null;

        Property property = element.getAnnotation(Property.class);
        String key = property.value();

        for (Annotation annotation : element.getAnnotations()) {

            if (annotation.annotationType().equals(Provider.class)) {
                Provider prov = (Provider) annotation;
                if (prov.value() != null && !prov.value().isBlank()) {
                    provider = prov.value();
                }
            }
        }

        IPropertySupplierBuilder<?> beanSupplierBuilder = Properties.property(elementType);
        if (provider != null && !provider.isEmpty()) {
            beanSupplierBuilder.provider(provider);
        }

        beanSupplierBuilder.key(key);

        IObjectSupplierBuilder<?, IObjectSupplier<?>> result = (IObjectSupplierBuilder) beanSupplierBuilder;

        return Optional.of(result);
    }
}
