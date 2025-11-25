package com.garganttua.core.injection.context.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InjectableElementResolver implements IInjectableElementResolver {

    private Map<Class<? extends Annotation>, IElementResolver> resolvers = new HashMap<>();

    public InjectableElementResolver(Map<Class<? extends Annotation>, IElementResolver> resolvers) {
        log.atTrace().log("Entering InjectableElementResolver constructor with resolvers map: {}", resolvers);
        this.resolvers.putAll(Objects.requireNonNull(resolvers, "Resolvers map cannot be null"));
        log.atDebug().log("Resolvers map initialized with {} entries", resolvers.size());
        log.atTrace().log("Exiting InjectableElementResolver constructor");
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        log.atTrace().log("Entering resolve with elementType: {} and element: {}", elementType, element);
        Objects.requireNonNull(element, "Element cannot be null");
        Objects.requireNonNull(elementType, "ElementType cannot be null");

        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            IElementResolver resolver = this.resolvers.get(type);
            log.atDebug().log("Checking resolver for annotation: {}", type.getSimpleName());
            if (resolver != null) {
                log.atInfo().log("Found resolver for annotation: {}, delegating resolve", type.getSimpleName());
                Resolved resolved = resolver.resolve(elementType, element);
                log.atTrace().log("Resolved result: {}", resolved);
                return resolved;
            }
        }

        boolean nullable = isNullable(element);
        log.atInfo().log("No specific resolver found, returning default Resolved (nullable: {}) for elementType: {}", nullable, elementType.getSimpleName());
        Resolved resolved = new Resolved(false, elementType, null, nullable);
        log.atTrace().log("Exiting resolve with Resolved: {}", resolved);
        return resolved;
    }

    @Override
    public Set<Resolved> resolve(Executable executable) throws DiException {
        log.atTrace().log("Entering resolve for Executable: {}", executable);
        Set<Resolved> paramResolved = new LinkedHashSet<>();
        for (Parameter parameter : executable.getParameters()) {
            log.atDebug().log("Resolving parameter: {} of type {}", parameter.getName(), parameter.getType().getSimpleName());
            paramResolved.add(resolve(parameter.getType(), parameter));
        }
        log.atTrace().log("Exiting resolve for Executable with resolved parameters: {}", paramResolved);
        return paramResolved;
    }

    @Override
    public void addResolver(Class<? extends Annotation> annotation, IElementResolver resolver) {
        log.atTrace().log("Entering addResolver with annotation: {} and resolver: {}", annotation.getSimpleName(), resolver);
        Objects.requireNonNull(annotation, "Annotation cannot be null");
        Objects.requireNonNull(resolver, "Resolver cannot be null");
        this.resolvers.put(annotation, resolver);
        log.atInfo().log("Added resolver for annotation: {}", annotation.getSimpleName());
        log.atTrace().log("Exiting addResolver");
    }
}