package com.garganttua.core.injection;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;

/**
 * Builder interface for constructing injectable element resolvers with custom resolvers.
 *
 * <p>
 * {@code IInjectableElementResolverBuilder} provides a fluent API for building
 * {@link IInjectableElementResolver} instances with registered custom resolvers for
 * specific annotation types. This builder is linked to the {@link IInjectionContextBuilder},
 * allowing it to be part of a DI context construction chain.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build an element resolver with custom resolvers
 * IInjectionContextBuilder contextBuilder = ...;
 *
 * contextBuilder.withElementResolver()
 *     .withResolver(Property.class, (type, element) -> {
 *         Property prop = element.getAnnotation(Property.class);
 *         return resolveProperty(prop.value(), type);
 *     })
 *     .withResolver(Qualifier.class, (type, element) -> {
 *         Qualifier qual = element.getAnnotation(Qualifier.class);
 *         return resolveQualifiedBean(qual.value(), type);
 *     })
 *     .and()
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IInjectableElementResolver
 * @see IElementResolver
 * @see IInjectionContextBuilder
 * @see ILinkedBuilder
 */
public interface IInjectableElementResolverBuilder extends ILinkedBuilder<IInjectionContextBuilder, IInjectableElementResolver>, IObservableBuilder<IInjectableElementResolverBuilder, IInjectableElementResolver>{

    /**
     * Registers a custom resolver for a specific annotation type.
     *
     * <p>
     * When an element annotated with the specified annotation is encountered during
     * dependency resolution, the provided resolver will be invoked to determine how
     * to inject it. Multiple resolvers can be registered for different annotations.
     * </p>
     *
     * @param annotation the annotation class to associate with the resolver
     * @param resolver the resolver to handle elements with this annotation
     * @return this builder for method chaining
     */
    IInjectableElementResolverBuilder withResolver(Class<? extends Annotation> annotation, IElementResolver resolver);

}
