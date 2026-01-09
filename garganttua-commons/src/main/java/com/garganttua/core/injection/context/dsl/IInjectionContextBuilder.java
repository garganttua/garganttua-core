package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.nativve.INativeBuilder;

/**
 * Builder interface for constructing dependency injection contexts using a fluent DSL.
 *
 * <p>
 * {@code IInjectionContextBuilder} provides a comprehensive fluent API for building {@link IInjectionContext}
 * instances with bean providers, property providers, package scanning, element resolvers, and
 * child context factories. This builder is the main entry point for programmatically configuring
 * the dependency injection container.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a complete DI context
 * IInjectionContext context = InjectionContextBuilder.create()
 *     .withPackage("com.myapp.services")
 *     .beanProvider("default")
 *         .withBean(MyService.class)
 *             .strategy(BeanStrategy.SINGLETON)
 *             .and()
 *         .and()
 *     .propertyProvider("config")
 *         .withProperty("db.url", "jdbc:mysql://localhost/mydb")
 *         .and()
 *     .resolvers()
 *         .withResolver(Property.class, propertyResolver)
 *         .and()
 *     .childContextFactory(requestContextFactory)
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IInjectionContext
 * @see IBeanProviderBuilder
 * @see IPropertyProviderBuilder
 * @see IAutomaticBuilder
 */
public interface IInjectionContextBuilder extends INativeBuilder<IInjectionContextBuilder, IInjectionContext>,
        IObservableBuilder<IInjectionContextBuilder, IInjectionContext> {

    /**
     * Registers a bean provider with an existing builder.
     *
     * @param scope the provider name/scope
     * @param provider the bean provider builder to register
     * @return this builder for method chaining
     */
    IBeanProviderBuilder beanProvider(String scope, IBeanProviderBuilder provider);

    /**
     * Creates and registers a new bean provider with the specified name.
     *
     * @param provider the provider name/scope
     * @return a bean provider builder for configuring the provider
     */
    IBeanProviderBuilder beanProvider(String provider);

    /**
     * Registers a property provider with an existing builder.
     *
     * @param scope the provider name/scope
     * @param provider the property provider builder to register
     * @return this builder for method chaining
     */
    IPropertyProviderBuilder propertyProvider(String scope, IPropertyProviderBuilder provider);

    /**
     * Creates and registers a new property provider with the specified name.
     *
     * @param provider the provider name/scope
     * @return a property provider builder for configuring the provider
     */
    IPropertyProviderBuilder propertyProvider(String provider);

    /**
     * Registers a child context factory for creating child contexts.
     *
     * @param factory the child context factory to register
     * @return this builder for method chaining
     */
    IInjectionContextBuilder childContextFactory(IInjectionChildContextFactory<? extends IInjectionContext> factory);

    /**
     * Accesses the injectable element resolver builder for registering custom resolvers.
     *
     * @return an element resolver builder
     */
    IInjectableElementResolverBuilder resolvers();

    /**
     * Registers a qualifier annotation type for use in bean resolution.
     *
     * @param qualifier the qualifier annotation class
     * @return this builder for method chaining
     */
    IInjectionContextBuilder withQualifier(Class<? extends Annotation> qualifier);

}
