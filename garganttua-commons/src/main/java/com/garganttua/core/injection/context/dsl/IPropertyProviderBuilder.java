package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.reflection.IClass;

/**
 * Builder interface for constructing property providers with configuration properties.
 *
 * <p>
 * {@code IPropertyProviderBuilder} provides a fluent API for building {@link IPropertyProvider}
 * instances with typed configuration properties. Properties registered through this builder
 * become available for injection into beans using the {@code @Property} annotation. This builder
 * is linked to {@link IInjectionContextBuilder}, enabling seamless integration into the context building
 * chain. Property providers act as configuration sources for the DI system.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a property provider with various typed properties
 * IInjectionContext context = InjectionContextBuilder.create()
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
 * IInjectionContext context = InjectionContextBuilder.create()
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
 * @see IInjectionContextBuilder
 * @see com.garganttua.core.injection.annotations.Property
 * @see IAutomaticLinkedBuilder
 */
public interface IPropertyProviderBuilder extends IAutomaticLinkedBuilder<IPropertyProviderBuilder, IInjectionContextBuilder, IPropertyProvider>{

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
    <PropertyType> IPropertyProviderBuilder withProperty(IClass<PropertyType> propertyType, String key, PropertyType property) throws DslException;

    /**
     * Registers a string-valued property in this provider.
     *
     * <p>
     * Convenience overload for the common case where the value originates as text — notably
     * when populating the builder from a configuration file. The value is stored as a
     * {@link String}; the backing provider coerces it to the requested scalar type on read
     * (see {@code getProperty(key, type)}), so typed lookups such as
     * {@code getProperty("app.port", Integer.class)} still work.
     * </p>
     *
     * @param key the property key for lookup (e.g., "database.url", "app.port")
     * @param value the property value as text
     * @return this builder for method chaining
     * @throws DslException if the property cannot be registered
     */
    IPropertyProviderBuilder withProperty(String key, String value) throws DslException;

}
