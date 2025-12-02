package com.garganttua.core.injection;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.utils.Copyable;

/**
 * Provides access to configuration properties within the dependency injection context.
 *
 * <p>
 * An {@code IPropertyProvider} acts as a repository for configuration properties, offering
 * type-safe property retrieval with automatic type conversion. It supports both mutable
 * and immutable property sources, manages property lifecycles, and can be copied to create
 * isolated property scopes.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Retrieve properties with type conversion
 * IPropertyProvider provider = ...;
 *
 * // Get a string property
 * Optional<String> dbUrl = provider.getProperty("database.url", String.class);
 *
 * // Get an integer property
 * Optional<Integer> poolSize = provider.getProperty("database.pool.size", Integer.class);
 *
 * // Set a property (if mutable)
 * if (provider.isMutable()) {
 *     provider.setProperty("cache.enabled", true);
 * }
 *
 * // List all property keys
 * Set<String> allKeys = provider.keys();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the mutability of the provider. Immutable providers are typically
 * thread-safe, while mutable providers may require external synchronization.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IDiContext
 * @see ILifecycle
 * @see Copyable
 */
public interface IPropertyProvider extends ILifecycle, Copyable<IPropertyProvider> {

    /**
     * Retrieves a property value with automatic type conversion.
     *
     * <p>
     * This method looks up the property by key and attempts to convert it to the
     * specified type. If the property does not exist or cannot be converted,
     * an empty Optional is returned.
     * </p>
     *
     * @param <T> the expected property type
     * @param key the property key
     * @param type the class of the expected type
     * @return an {@link Optional} containing the property value if found, or empty otherwise
     * @throws DiException if an error occurs during property retrieval or type conversion
     */
    <T> Optional<T> getProperty(String key, Class<T> type) throws DiException;

    /**
     * Sets a property value in this provider.
     *
     * <p>
     * This method is only supported for mutable providers. Attempting to set
     * a property on an immutable provider will throw an exception.
     * </p>
     *
     * @param key the property key
     * @param value the property value
     * @throws DiException if the provider is immutable or an error occurs
     */
    void setProperty(String key, Object value) throws DiException;

    /**
     * Checks if this provider is mutable.
     *
     * <p>
     * Mutable providers allow modification of their properties at runtime,
     * while immutable providers have a fixed set of properties.
     * </p>
     *
     * @return {@code true} if the provider is mutable, {@code false} otherwise
     */
    boolean isMutable();

    /**
     * Returns all property keys available in this provider.
     *
     * @return a set of all property keys (never {@code null}, may be empty)
     */
    Set<String> keys();
}
