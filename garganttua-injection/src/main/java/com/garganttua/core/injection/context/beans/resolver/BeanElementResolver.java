package com.garganttua.core.injection.context.beans.resolver;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.annotations.Provider;
import com.garganttua.core.injection.context.beans.Beans;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Base resolver that turns an injectable element into a bean {@link ISupplierBuilder},
 * reading {@code @Named}, {@code @Provider} and qualifier annotations from the element.
 * Concrete subclasses fix the {@link BeanStrategy} (singleton or prototype).
 */
public class BeanElementResolver {
    private static final Logger log = Logger.getLogger(BeanElementResolver.class);

    private Set<IClass<? extends Annotation>> qualifiers;

    /**
     * Builds a resolver aware of the given qualifier annotation types.
     *
     * @param qualifiers annotation types treated as bean qualifiers
     */
    protected BeanElementResolver(Set<IClass<? extends Annotation>> qualifiers) {
        log.trace("Entering BeanElementResolver constructor with qualifiers: {}", qualifiers);
        this.qualifiers = Objects.requireNonNull(qualifiers, "Qualifiers cannot be null");
        log.debug("BeanElementResolver initialized with qualifiers: {}", qualifiers);
        log.trace("Exiting BeanElementResolver constructor");
    }

    /**
     * Builds a bean supplier builder for the element, applying the given strategy and the
     * name/provider/qualifiers parsed from the element's annotations.
     *
     * @param elementType the injection target type
     * @param parameter   the annotated element being injected
     * @param strategy    the bean strategy to request, may be {@code null}
     * @return a supplier builder for the resolved bean, always present
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Optional<ISupplierBuilder<?, ISupplier<?>>> resolve(IClass<?> elementType,
            IAnnotatedElement parameter,
            BeanStrategy strategy) {
        log.trace("Entering resolve with elementType: {}, parameter: {}, strategy: {}", elementType, parameter,
                strategy);

        Objects.requireNonNull(parameter, "Parameter cannot be null");
        Objects.requireNonNull(elementType, "Element type cannot be null");

        BeanAnnotations parsed = parseBeanAnnotations(parameter);

        ISupplierBuilder beanSupplierBuilder = Beans.bean(
                Optional.ofNullable(parsed.provider()),
                new BeanReference<>(elementType, Optional.ofNullable(strategy), Optional.ofNullable(parsed.name()),
                        parsed.qualifiers()));

        log.debug("Bean supplier builder created for elementType: {} with provider: {} and name: {}",
                elementType, parsed.provider(), parsed.name());
        log.trace("Exiting resolve with builder: {}", beanSupplierBuilder);

        return Optional.of(beanSupplierBuilder);
    }

    /** Extract the @Named name, @Provider provider and matching qualifier annotations from the element. */
    private BeanAnnotations parseBeanAnnotations(IAnnotatedElement parameter) {
        String name = null;
        String provider = null;
        Set<IClass<? extends Annotation>> paramQualifiers = new HashSet<>();
        for (Annotation annotation : parameter.getAnnotations()) {
            log.debug("Inspecting annotation: {}", annotation);
            if (annotation.annotationType().equals(Named.class)) {
                Named named = (Named) annotation;
                if (named.value() != null && !named.value().isBlank()) {
                    name = named.value();
                }
            } else if (annotation.annotationType().equals(Provider.class)) {
                Provider prov = (Provider) annotation;
                if (prov.value() != null && !prov.value().isBlank()) {
                    provider = prov.value();
                }
            } else {
                @SuppressWarnings("unchecked")
                IClass<? extends Annotation> annotationIClass =
                        (IClass<? extends Annotation>) IClass.getClass(annotation.annotationType());
                if (qualifiers.stream().anyMatch(q -> q.getName().equals(annotationIClass.getName()))) {
                    paramQualifiers.add(annotationIClass);
                }
            }
        }
        return new BeanAnnotations(name, provider, paramQualifiers);
    }

    private record BeanAnnotations(String name, String provider, Set<IClass<? extends Annotation>> qualifiers) {
    }
}
