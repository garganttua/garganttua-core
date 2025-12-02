package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.IPropertyProvider;

/**
 * Builder interface for constructing property providers with configuration properties.
 *
 * <p>
 * {@code IPropertyProviderBuilder} provides a fluent API for building {@link IPropertyProvider}
 * instances with typed configuration properties. Properties registered through this builder
 * become available for injection into beans using the {@code @Property} annotation. This builder
 * is linked to {@link IDiContextBuilder}, enabling seamless integration into the context building
 * chain. Property providers act as configuration sources for the DI system.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a property provider with various typed properties
 * IDiContext context = DiContextBuilder.create()
 *     .propertyProvider("config")
 *         .withProperty(String.class, "database.url", "jdbc:mysql://localhost/mydb")
 *         .withProperty(Integer.class, "database.pool.size", 10)
 *         .withProperty(Boolean.class, "database.pool.enabled", true)
 *         .withProperty(String.class, "app.name", "MyApplication")
 *         .withProperty(Integer.class, "app.port", 8080)
 *         .and()
 *     .build();
 *
 * // Multiple property providers for different concerns
 * IDiContext context = DiContextBuilder.create()
 *     .propertyProvider("database")
 *         .withProperty(String.class, "url", "jdbc:mysql://localhost/mydb")
 *         .withProperty(Integer.class, "pool.size", 10)
 *         .and()
 *     .propertyProvider("security")
 *         .withProperty(String.class, "jwt.secret", "secret-key")
 *         .withProperty(Integer.class, "token.expiry", 3600)
 *         .and()
 *     .build();
 *
 * // Properties are then injectable
 * public class DatabaseService {
 *     @Property("database.url")
 *     private String databaseUrl;
 *
 *     @Property("database.pool.size")
 *     private int poolSize;
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IPropertyProvider
 * @see IDiContextBuilder
 * @see com.garganttua.core.injection.annotations.Property
 * @see IAutomaticLinkedBuilder
 */
public interface IPropertyProviderBuilder extends IAutomaticLinkedBuilder<IPropertyProviderBuilder, IDiContextBuilder, IPropertyProvider>{

    /**
     * Registers a typed property in this provider.
     *
     * <p>
     * This method adds a configuration property with a specific type, key, and value to
     * the property provider. The property type ensures type-safe injection and automatic
     * type conversion when the property is requested. The key is used for property lookup
     * during injection.
     * </p>
     *
     * @param <PropertyType> the type of the property value
     * @param propertyType the class of the property type
     * @param key the property key for lookup (e.g., "database.url", "app.port")
     * @param property the property value
     * @return this builder for method chaining
     * @throws DslException if the property cannot be registered
     */
    <PropertyType> IPropertyProviderBuilder withProperty(Class<PropertyType> propertyType, String key, PropertyType property) throws DslException;

}
