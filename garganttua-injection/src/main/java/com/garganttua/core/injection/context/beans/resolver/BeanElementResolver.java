package com.garganttua.core.injection.context.beans.resolver;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.context.beans.Beans;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanElementResolver {

    private Set<IClass<? extends Annotation>> qualifiers;

    protected BeanElementResolver(Set<IClass<? extends Annotation>> qualifiers) {
        log.atTrace().log("Entering BeanElementResolver constructor with qualifiers: {}", qualifiers);
        this.qualifiers = Objects.requireNonNull(qualifiers, "Qualifiers cannot be null");
        log.atDebug().log("BeanElementResolver initialized with qualifiers: {}", qualifiers);
        log.atTrace().log("Exiting BeanElementResolver constructor");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Optional<ISupplierBuilder<?, ISupplier<?>>> resolve(IClass<?> elementType,
            IAnnotatedElement parameter,
            BeanStrategy strategy) {
        log.atTrace().log("Entering resolve with elementType: {}, parameter: {}, strategy: {}", elementType, parameter,
                strategy);

        Objects.requireNonNull(parameter, "Parameter cannot be null");
        Objects.requireNonNull(elementType, "Element type cannot be null");

        String name = null;
        String provider = null;
        Set<IClass<? extends Annotation>> paramQualifiers = new HashSet<>();

        for (Annotation annotation : parameter.getAnnotations()) {
            log.atDebug().log("Inspecting annotation: {}", annotation);

            if (annotation.annotationType().equals(Named.class)) {
                Named named = (Named) annotation;
                if (named.value() != null && !named.value().isBlank()) {
                    name = named.value();
                    log.atDebug().log("Named annotation found with value: {}", name);
                }
            } else if (annotation.annotationType().equals(Provider.class)) {
                Provider prov = (Provider) annotation;
                if (prov.value() != null && !prov.value().isBlank()) {
                    provider = prov.value();
                    log.atDebug().log("Provider annotation found with value: {}", provider);
                }
            } else {
                @SuppressWarnings("unchecked")
                IClass<? extends Annotation> annotationIClass = (IClass<? extends Annotation>) IClass.getClass(annotation.annotationType());
                if (qualifiers.stream().anyMatch(q -> q.getName().equals(annotationIClass.getName()))) {
                    paramQualifiers.add(annotationIClass);
                    log.atDebug().log("Qualifier annotation found: {}", annotation.annotationType());
                }
            }
        }

        ISupplierBuilder beanSupplierBuilder = Beans.bean(
                Optional.ofNullable(provider),
                new BeanReference<>(elementType, Optional.ofNullable(strategy), Optional.ofNullable(name),
                        paramQualifiers));

        log.atDebug().log("Bean supplier builder created for elementType: {} with provider: {} and name: {}",
                elementType, provider, name);
        log.atTrace().log("Exiting resolve with builder: {}", beanSupplierBuilder);

        return Optional.of(beanSupplierBuilder);
    }
}
