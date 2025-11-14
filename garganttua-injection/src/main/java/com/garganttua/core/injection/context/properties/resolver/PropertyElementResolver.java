package com.garganttua.core.injection.context.properties.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;
import com.garganttua.core.injection.context.properties.Properties;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class PropertyElementResolver implements IElementResolver {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Resolved resolve(Class<?> elementType,
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

        IPropertySupplierBuilder<?> propertySupplierBuilder = Properties.property(elementType);
        if (provider != null && !provider.isEmpty()) {
            propertySupplierBuilder.provider(provider);
        }

        propertySupplierBuilder.key(key);

        IObjectSupplierBuilder<?, IObjectSupplier<?>> result = (IObjectSupplierBuilder) propertySupplierBuilder;

        return new Resolved(true, elementType, result, IInjectableElementResolver.isNullable(element));
    }
}
