package com.garganttua.core.injection.context.properties.resolver;

import java.lang.annotation.Annotation;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;
import com.garganttua.core.injection.context.properties.Properties;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * {@link IElementResolver} for elements annotated with {@code @Property}, producing a
 * property supplier keyed by the annotation's value and optionally scoped to a {@code @Provider}.
 */
@Resolver(annotations={Property.class})
public class PropertyElementResolver implements IElementResolver {
    /** Creates a property element resolver. */
    public PropertyElementResolver() {
    }

    private static final Logger log = Logger.getLogger(PropertyElementResolver.class);

    /**
     * Resolves a {@code @Property}-annotated element to a property supplier builder.
     *
     * @param elementType the declared type of the element; must not be {@code null}
     * @param element     the annotated element; must not be {@code null}
     * @return the resolution result wrapping a property supplier builder
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) {
        log.trace("Entering resolve with elementType: {} and element: {}", elementType, element);

        Objects.requireNonNull(element, "Element cannot be null");
        log.debug("Element is not null: {}", element);

        Objects.requireNonNull(elementType, "ElementType cannot be null");
        log.debug("ElementType is not null: {}", elementType);

        Property property = element.getAnnotation(IClass.getClass(Property.class));
        log.debug("Retrieved @Property annotation: {}", property);

        String key = property.value();
        log.debug("Property key: {}", key);

        String provider = resolveProvider(element);

        IPropertySupplierBuilder<?> propertySupplierBuilder = Properties.property(elementType);
        log.debug("Created IPropertySupplierBuilder for elementType: {}", elementType.getSimpleName());

        if (provider != null && !provider.isEmpty()) {
            propertySupplierBuilder.provider(provider);
            log.debug("Set provider '{}' on propertySupplierBuilder", provider);
        }

        propertySupplierBuilder.key(key);
        log.debug("Set key '{}' on propertySupplierBuilder", key);

        ISupplierBuilder<?, ISupplier<?>> result = (ISupplierBuilder) propertySupplierBuilder;
        log.trace("Exiting resolve with Resolved for elementType: {}", elementType.getSimpleName());

        return new Resolved(true, elementType, result, IInjectableElementResolver.isNullable(element));
    }

    /** Resolve the optional {@code @Provider} value declared on the element, or {@code null} if none/blank. */
    private String resolveProvider(IAnnotatedElement element) {
        String provider = null;
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().equals(Provider.class)) {
                Provider prov = (Provider) annotation;
                if (prov.value() != null && !prov.value().isBlank()) {
                    provider = prov.value();
                    log.debug("Found provider annotation with value: {}", provider);
                } else {
                    log.debug("Provider annotation value is null or blank");
                }
            } else {
                log.trace("Skipping unrelated annotation: {}", annotation.annotationType().getSimpleName());
            }
        }
        return provider;
    }
}
