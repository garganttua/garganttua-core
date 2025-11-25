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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyElementResolver implements IElementResolver {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) {
        log.atTrace().log("Entering resolve with elementType: {} and element: {}", elementType, element);

        Objects.requireNonNull(element, "Element cannot be null");
        log.atDebug().log("Element is not null: {}", element);

        Objects.requireNonNull(elementType, "ElementType cannot be null");
        log.atDebug().log("ElementType is not null: {}", elementType);

        String provider = null;

        Property property = element.getAnnotation(Property.class);
        log.atDebug().log("Retrieved @Property annotation: {}", property);

        String key = property.value();
        log.atInfo().log("Property key: {}", key);

        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().equals(Provider.class)) {
                Provider prov = (Provider) annotation;
                if (prov.value() != null && !prov.value().isBlank()) {
                    provider = prov.value();
                    log.atInfo().log("Found provider annotation with value: {}", provider);
                } else {
                    log.atDebug().log("Provider annotation value is null or blank");
                }
            } else {
                log.atTrace().log("Skipping unrelated annotation: {}", annotation.annotationType().getSimpleName());
            }
        }

        IPropertySupplierBuilder<?> propertySupplierBuilder = Properties.property(elementType);
        log.atDebug().log("Created IPropertySupplierBuilder for elementType: {}", elementType.getSimpleName());

        if (provider != null && !provider.isEmpty()) {
            propertySupplierBuilder.provider(provider);
            log.atInfo().log("Set provider '{}' on propertySupplierBuilder", provider);
        }

        propertySupplierBuilder.key(key);
        log.atInfo().log("Set key '{}' on propertySupplierBuilder", key);

        IObjectSupplierBuilder<?, IObjectSupplier<?>> result = (IObjectSupplierBuilder) propertySupplierBuilder;
        log.atTrace().log("Exiting resolve with Resolved for elementType: {}", elementType.getSimpleName());

        return new Resolved(true, elementType, result, IInjectableElementResolver.isNullable(element));
    }
}
