package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

/**
 * Builder interface for constructing property suppliers with query criteria.
 *
 * <p>
 * {@code IPropertySupplierBuilder} provides a fluent API for building {@link IPropertySupplier}
 * instances that can dynamically resolve and supply property values from property providers
 * based on specified key and provider criteria. This builder extends {@link IObjectSupplierBuilder}
 * with property-specific query capabilities, enabling late binding and dynamic property resolution.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a property supplier with key
 * IPropertySupplier<String> dbUrlSupplier = propertySupplierBuilder
 *     .key("database.url")
 *     .build();
 *
 * // Use the supplier to resolve the property
 * String databaseUrl = dbUrlSupplier.supply(context);
 *
 * // Property supplier with specific provider
 * IPropertySupplier<Integer> portSupplier = propertySupplierBuilder
 *     .key("server.port")
 *     .provider("config")
 *     .build();
 *
 * Integer serverPort = portSupplier.supply(context);
 *
 * // Use in bean configuration
 * beanFactoryBuilder
 *     .constructor()
 *         .withParameter(String.class)
 *             .supplier(propertySupplierBuilder
 *                 .key("database.url")
 *                 .build())
 *             .and()
 *         .and();
 * }</pre>
 *
 * @param <Property> the type of property value this supplier provides
 * @since 2.0.0-ALPHA01
 * @see IPropertySupplier
 * @see IObjectSupplierBuilder
 * @see com.garganttua.core.injection.IPropertyProvider
 */
public interface IPropertySupplierBuilder<Property> extends IObjectSupplierBuilder<Property, IPropertySupplier<Property>>  {

    /**
     * Specifies the property key to search for.
     *
     * <p>
     * The key is used to lookup the property value from the property providers.
     * This follows the same key naming convention as {@code @Property} annotation.
     * </p>
     *
     * @param name the property key
     * @return this builder for method chaining
     */
    IPropertySupplierBuilder<Property> key(String name);

    /**
     * Specifies the property provider from which to retrieve the property.
     *
     * <p>
     * If not specified, the property will be searched across all property providers.
     * Specifying a provider narrows the search to a specific property provider.
     * </p>
     *
     * @param provider the property provider name
     * @return this builder for method chaining
     */
    IPropertySupplierBuilder<Property> provider(String provider);

}
