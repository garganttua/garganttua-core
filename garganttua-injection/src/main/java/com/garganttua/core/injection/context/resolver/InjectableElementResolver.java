package com.garganttua.core.injection.context.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IExecutable;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IParameter;

/**
 * Default {@link IInjectableElementResolver} that dispatches resolution to per-annotation
 * {@link IElementResolver}s, keyed by annotation type name.
 */
public class InjectableElementResolver implements IInjectableElementResolver {
    private static final Logger log = Logger.getLogger(InjectableElementResolver.class);

    private Map<String, IElementResolver> resolvers = new ConcurrentHashMap<>();

    /**
     * Creates a resolver seeded with the given annotation-to-resolver mappings.
     *
     * @param resolvers initial map of annotation types to their element resolvers
     */
    public InjectableElementResolver(Map<IClass<? extends Annotation>, IElementResolver> resolvers) {
        log.trace("Entering InjectableElementResolver constructor with resolvers map: {}", resolvers);
        Objects.requireNonNull(resolvers, "Resolvers map cannot be null");
        resolvers.forEach((k, v) -> this.resolvers.put(k.getName(), v));
        log.debug("Resolvers map initialized with {} entries", resolvers.size());
        log.trace("Exiting InjectableElementResolver constructor");
    }

    /**
     * Resolves a single element by delegating to the resolver registered for the first of the
     * element's annotations that has one.
     *
     * @return the delegate's result, or an unresolved {@link Resolved} (carrying the element's
     *         nullability) when no annotation matches a registered resolver
     */
    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {
        log.trace("Entering resolve with elementType: {} and element: {}", elementType, element);
        Objects.requireNonNull(element, "Element cannot be null");
        Objects.requireNonNull(elementType, "ElementType cannot be null");

        for (Annotation annotation : element.getAnnotations()) {
            String typeName = annotation.annotationType().getName();
            IElementResolver resolver = this.resolvers.get(typeName);
            log.debug("Checking resolver for annotation: {}", annotation.annotationType().getSimpleName());
            if (resolver != null) {
                log.debug("Found resolver for annotation: {}, delegating resolve", annotation.annotationType().getSimpleName());
                Resolved resolved = resolver.resolve(elementType, element);
                log.trace("Resolved result: {}", resolved);
                return resolved;
            }
        }

        boolean nullable = isNullable(element);
        log.debug("No specific resolver found, returning default Resolved (nullable: {}) for elementType: {}", nullable, elementType.getSimpleName());
        Resolved resolved = new Resolved(false, elementType, null, nullable);
        log.trace("Exiting resolve with Resolved: {}", resolved);
        return resolved;
    }

    /**
     * Resolves every parameter of a constructor or method.
     *
     * @param executable the executable whose parameters are resolved
     * @return one {@link Resolved} per parameter, or an empty set for unsupported executables
     */
    @Override
    public Set<Resolved> resolve(IExecutable executable) throws DiException {
        log.trace("Entering resolve for IExecutable: {}", executable);
        IParameter[] parameters;
        if (executable instanceof IConstructor<?> c) {
            parameters = c.getParameters();
        } else if (executable instanceof IMethod m) {
            parameters = m.getParameters();
        } else {
            return new LinkedHashSet<>();
        }
        Set<Resolved> paramResolved = new LinkedHashSet<>();
        for (IParameter parameter : parameters) {
            log.debug("Resolving parameter: {} of type {}", parameter.getName(), parameter.getType().getSimpleName());
            paramResolved.add(resolve(parameter.getType(), parameter));
        }
        log.trace("Exiting resolve for IExecutable with resolved parameters: {}", paramResolved);
        return paramResolved;
    }

    /**
     * Registers (or replaces) the resolver handling a given annotation type.
     *
     * @param annotation the annotation type to bind
     * @param resolver the resolver invoked for elements carrying that annotation
     */
    @Override
    public void addResolver(IClass<? extends Annotation> annotation, IElementResolver resolver) {
        log.trace("Entering addResolver with annotation: {} and resolver: {}", annotation.getSimpleName(), resolver);
        Objects.requireNonNull(annotation, "Annotation cannot be null");
        Objects.requireNonNull(resolver, "Resolver cannot be null");
        this.resolvers.put(annotation.getName(), resolver);
        log.debug("Added resolver for annotation: {}", annotation.getSimpleName());
        log.trace("Exiting addResolver");
    }
}
