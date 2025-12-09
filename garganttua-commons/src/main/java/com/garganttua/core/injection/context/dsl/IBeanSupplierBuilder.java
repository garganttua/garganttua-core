package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;

import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder interface for constructing bean suppliers with query criteria.
 *
 * <p>
 * {@code IBeanSupplierBuilder} provides a fluent API for building {@link IBeanSupplier} instances
 * that can dynamically resolve and supply beans based on specified criteria such as name, provider,
 * strategy, and qualifiers. This builder extends {@link ISupplierBuilder} with DI-specific
 * query capabilities and tracks dependencies for circular dependency detection. Bean suppliers
 * enable late binding and dynamic bean resolution.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a bean supplier with query criteria
 * IBeanSupplier<DataSource> dataSourceSupplier = supplierBuilder
 *     .name("primaryDataSource")
 *     .provider("application")
 *     .strategy(BeanStrategy.singleton)
 *     .qualifier(Primary.class)
 *     .build();
 *
 * // Use the supplier to resolve the bean
 * DataSource dataSource = dataSourceSupplier.supply(context);
 *
 * // Supplier for prototype beans
 * IBeanSupplier<RequestHandler> handlerSupplier = supplierBuilder
 *     .strategy(BeanStrategy.prototype)
 *     .build();
 *
 * // Each call creates a new instance
 * RequestHandler handler1 = handlerSupplier.supply(context);
 * RequestHandler handler2 = handlerSupplier.supply(context);
 * // handler1 != handler2
 * }</pre>
 *
 * @param <Bean> the type of bean this supplier provides
 * @since 2.0.0-ALPHA01
 * @see IBeanSupplier
 * @see ISupplierBuilder
 * @see Dependent
 */
public interface IBeanSupplierBuilder<Bean> extends ISupplierBuilder<Bean, IBeanSupplier<Bean>>, Dependent {

    /**
     * Specifies the bean name to search for.
     *
     * @param name the bean name
     * @return this builder for method chaining
     */
    IBeanSupplierBuilder<Bean> name(String name);

    /**
     * Specifies the provider from which to retrieve the bean.
     *
     * @param provider the provider name/scope
     * @return this builder for method chaining
     */
    IBeanSupplierBuilder<Bean> provider(String provider);

    /**
     * Specifies the bean strategy (scope) to search for.
     *
     * @param strategy the bean strategy
     * @return this builder for method chaining
     */
    IBeanSupplierBuilder<Bean> strategy(BeanStrategy strategy);

    /**
     * Adds a qualifier annotation to the search criteria.
     *
     * @param qualifier the qualifier annotation class
     * @return this builder for method chaining
     */
    IBeanSupplierBuilder<Bean> qualifier(Class<? extends Annotation> qualifier);

}
