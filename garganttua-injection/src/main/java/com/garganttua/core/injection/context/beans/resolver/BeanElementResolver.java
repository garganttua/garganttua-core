package com.garganttua.core.injection.context.beans.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.context.beans.Beans;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class BeanElementResolver {

    private Set<Class<? extends Annotation>> qualifiers;

    protected BeanElementResolver(Set<Class<? extends Annotation>> qualifiers) {
        this.qualifiers = Objects.requireNonNull(qualifiers, "Qualifiers cannot be null");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> resolve(Class<?> elementType, AnnotatedElement parameter,
            BeanStrategy strategy) {
        Objects.requireNonNull(parameter, "Parameter cannot be null");
        Objects.requireNonNull(elementType, "Element type cannot be null");

        String name = null;
        String provider = null;
        Set<Class<? extends Annotation>> paramQualifiers = new HashSet<>();

        for (Annotation annotation : parameter.getAnnotations()) {

            if (annotation.annotationType().equals(Named.class)) {
                Named named = (Named) annotation;
                if (named.value() != null && !named.value().isBlank()) {
                    name = named.value();
                }
            }

            else if (annotation.annotationType().equals(Provider.class)) {
                Provider prov = (Provider) annotation;
                if (prov.value() != null && !prov.value().isBlank()) {
                    provider = prov.value();
                }
            }

            else if (qualifiers.contains(annotation.getClass())) {
                paramQualifiers.add(annotation.getClass());
            }
        }

        IObjectSupplierBuilder beanSupplierBuilder = Beans.bean(Optional.ofNullable(provider),
                BeanDefinition.example(elementType, Optional.ofNullable(strategy), Optional.ofNullable(name), paramQualifiers));

        return Optional.of(beanSupplierBuilder);
    }

}
