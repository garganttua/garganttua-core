package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.IBeanProvider;

/**
 * Builder interface for constructing bean providers with registered beans and package scanning.
 *
 * <p>
 * {@code IBeanProviderBuilder} provides a fluent API for building {@link IBeanProvider} instances
 * with beans registered either explicitly through {@link #withBean(Class)} or discovered automatically
 * through package scanning with {@link #withPackage(String)}. This builder is linked to
 * {@link IDiContextBuilder}, allowing seamless integration into the context building chain.
 * Bean providers act as scopes or namespaces for organizing beans within the DI context.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a bean provider with explicit beans
 * IDiContext context = DiContextBuilder.create()
 *     .beanProvider("application")
 *         .withBean(DatabaseConnection.class)
 *             .strategy(BeanStrategy.singleton)
 *             .name("mainDatabase")
 *             .and()
 *         .withBean(CacheService.class)
 *             .strategy(BeanStrategy.singleton)
 *             .and()
 *         .and()
 *     .build();
 *
 * // Build with package scanning
 * IDiContext context = DiContextBuilder.create()
 *     .beanProvider("services")
 *         .withPackage("com.myapp.services")
 *         .withPackages(new String[]{"com.myapp.controllers", "com.myapp.repositories"})
 *         .and()
 *     .build();
 *
 * // Mixed approach
 * IDiContext context = DiContextBuilder.create()
 *     .beanProvider("default")
 *         .withPackage("com.myapp")
 *         .withBean(CustomConfig.class)
 *             .strategy(BeanStrategy.singleton)
 *             .and()
 *         .and()
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IBeanProvider
 * @see IBeanFactoryBuilder
 * @see IDiContextBuilder
 * @see IAutomaticLinkedBuilder
 */
public interface IBeanProviderBuilder extends IAutomaticLinkedBuilder<IBeanProviderBuilder, IDiContextBuilder, IBeanProvider> {

    /**
     * Registers a bean explicitly in this provider.
     *
     * <p>
     * This method begins the configuration of a bean factory for the specified bean type.
     * The returned builder allows detailed configuration of the bean including strategy,
     * name, qualifiers, constructor, fields, and post-construct methods.
     * </p>
     *
     * @param <BeanType> the type of bean to register
     * @param beanType the class of the bean
     * @return a bean factory builder for configuring the bean
     * @throws DslException if the bean cannot be registered or configured
     */
    <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType) throws DslException;

    /**
     * Adds a package to scan for beans with DI annotations.
     *
     * <p>
     * Classes in the specified package and its sub-packages that are annotated with
     * bean annotations (like {@code @Singleton}, {@code @Prototype}, etc.) will be
     * automatically registered as beans in this provider.
     * </p>
     *
     * @param packageName the package name to scan
     * @return this builder for method chaining
     */
    IBeanProviderBuilder withPackage(String packageName);

    /**
     * Adds multiple packages to scan for beans with DI annotations.
     *
     * <p>
     * Classes in the specified packages and their sub-packages that are annotated with
     * bean annotations will be automatically registered as beans in this provider.
     * </p>
     *
     * @param packageNames array of package names to scan
     * @return this builder for method chaining
     */
    IBeanProviderBuilder  withPackages(String[] packageNames);

}
