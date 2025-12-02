package com.garganttua.core.injection.context.dsl;

import java.util.Map;

import com.garganttua.core.dsl.IBuilder;

/**
 * Builder interface for constructing property entries in the dependency injection context.
 *
 * <p>
 * {@code IPropertyBuilder} provides a fluent API for building property key-value pairs that
 * will be added to a property provider. The built result is a Map.Entry containing the property
 * key and its typed value. This builder is typically used internally by {@link IPropertyProviderBuilder}
 * during property provider configuration.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Property builder is typically used internally
 * // But can be used directly if needed
 * IPropertyBuilder<String> propertyBuilder = ...;
 * Map.Entry<String, String> entry = propertyBuilder.build();
 * String key = entry.getKey();
 * String value = entry.getValue();
 *
 * // More commonly used through property provider builder
 * propertyProviderBuilder
 *     .withProperty(String.class, "database.url", "jdbc:mysql://localhost/mydb")
 *     .withProperty(Integer.class, "database.pool.size", 10);
 * }</pre>
 *
 * @param <PropertyType> the type of the property value
 * @since 2.0.0-ALPHA01
 * @see IPropertyProviderBuilder
 * @see IBuilder
 */
public interface IPropertyBuilder<PropertyType> extends IBuilder<Map.Entry<String, PropertyType>>{

}
