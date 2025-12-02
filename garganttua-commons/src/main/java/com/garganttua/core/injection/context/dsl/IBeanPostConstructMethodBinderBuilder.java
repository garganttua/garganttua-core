package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

/**
 * Builder interface for configuring post-construction methods to be invoked after bean instantiation.
 *
 * <p>
 * {@code IBeanPostConstructMethodBinderBuilder} provides a fluent API for configuring methods
 * that should be called automatically after a bean is instantiated and its dependencies are injected.
 * These post-construct methods enable initialization logic, resource acquisition, and validation
 * to be performed after the bean is fully constructed. This builder extends {@link IMethodBinderBuilder}
 * with DI-specific functionality and tracks dependencies for circular dependency detection.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Configure bean with post-construct method
 * beanProviderBuilder
 *     .withBean(DatabaseService.class)
 *         .strategy(BeanStrategy.singleton)
 *         .constructor()
 *             .withParameter(DataSource.class)
 *                 .fromBean().type(DataSource.class).and()
 *                 .and()
 *             .and()
 *         .postConstruction()
 *             .method("initialize")
 *             .and()
 *         .build();
 *
 * // Post-construct method with parameters
 * beanProviderBuilder
 *     .withBean(CacheService.class)
 *         .postConstruction()
 *             .method("setup")
 *             .withParameter(Integer.class)
 *                 .fromProperty()
 *                     .key("cache.size")
 *                     .and()
 *                 .and()
 *             .and()
 *         .build();
 *
 * // Multiple post-construct methods
 * beanProviderBuilder
 *     .withBean(ComplexService.class)
 *         .postConstruction()
 *             .method("initializeResources")
 *             .and()
 *         .postConstruction()
 *             .method("validateConfiguration")
 *             .and()
 *         .build();
 * }</pre>
 *
 * @param <Bean> the type of bean this post-construct builder is for
 * @since 2.0.0-ALPHA01
 * @see IMethodBinder
 * @see IBeanFactoryBuilder
 * @see Dependent
 */
public interface IBeanPostConstructMethodBinderBuilder<Bean> extends
        IMethodBinderBuilder<Void, IBeanPostConstructMethodBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>, IMethodBinder<Void>>, Dependent {

    /**
     * Builds the method binder with a specific bean supplier.
     *
     * <p>
     * This method creates a method binder configured to invoke the post-construct method
     * on beans provided by the specified supplier. This variant is used when the bean
     * supplier is known at build time.
     * </p>
     *
     * @param bean the bean supplier that provides instances for method invocation
     * @return the configured method binder
     * @throws DslException if the method binder cannot be built
     */
    IMethodBinder<Void> build(IObjectSupplierBuilder<Bean, IObjectSupplier<Bean>> bean) throws DslException;

}
