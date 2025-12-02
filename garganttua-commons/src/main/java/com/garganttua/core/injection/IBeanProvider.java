package com.garganttua.core.injection;

import java.util.List;
import java.util.Optional;

import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.nativve.INativeConfiguration;
import com.garganttua.core.nativve.INativeReflectionConfiguration;
import com.garganttua.core.utils.Copyable;

/**
 * Provides access to managed beans within a specific scope or provider context.
 *
 * <p>
 * An {@code IBeanProvider} acts as a repository for bean instances, offering various
 * query mechanisms to retrieve beans by type, name, or complete bean definition. It supports
 * both singleton and prototype scopes, manages bean lifecycles, and can be copied to create
 * isolated provider instances.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Retrieve a bean by type
 * IBeanProvider provider = ...;
 * Optional<MyService> service = provider.getBean(MyService.class);
 *
 * // Retrieve a named bean
 * Optional<DataSource> ds = provider.getBean("primaryDataSource", DataSource.class);
 *
 * // Query beans matching a definition
 * BeanDefinition<MyService> query = BeanDefinition.example(
 *     MyService.class,
 *     Optional.of(BeanStrategy.SINGLETON),
 *     Optional.empty(),
 *     Set.of(Qualifier.class));
 * List<MyService> services = provider.queryBeans(query);
 *
 * // Get all implementations of an interface
 * List<Plugin> plugins = provider.getBeansImplementingInterface(Plugin.class, false);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the mutability of the provider. Immutable providers are typically
 * thread-safe, while mutable providers may require external synchronization.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IBeanFactory
 * @see BeanDefinition
 * @see ILifecycle
 * @see Copyable
 */
public interface IBeanProvider extends ILifecycle, Copyable<IBeanProvider>, INativeReflectionConfiguration {

    /**
     * Retrieves a bean instance by its type.
     *
     * <p>
     * This method searches for a bean that is assignable to the specified type.
     * If multiple beans of the same type exist, the behavior is implementation-specific.
     * </p>
     *
     * @param <T> the bean type
     * @param type the class of the bean to retrieve
     * @return an {@link Optional} containing the bean if found, or empty if not found
     * @throws DiException if an error occurs during bean retrieval or instantiation
     */
    <T> Optional<T> getBean(Class<T> type) throws DiException;

    /**
     * Retrieves a bean instance by its name and type.
     *
     * <p>
     * This method performs a lookup using both the bean name and type, providing
     * more precise bean resolution when multiple beans of the same type exist.
     * </p>
     *
     * @param <T> the bean type
     * @param name the name of the bean
     * @param type the class of the bean to retrieve
     * @return an {@link Optional} containing the bean if found, or empty if not found
     * @throws DiException if an error occurs during bean retrieval or instantiation
     */
    <T> Optional<T> getBean(String name, Class<T> type) throws DiException;

    /**
     * Retrieves all beans that implement or extend the specified interface or class.
     *
     * <p>
     * This method allows discovering all beans compatible with a given type.
     * Prototype beans can optionally be included or excluded from the results.
     * </p>
     *
     * @param <T> the interface or superclass type
     * @param interfasse the interface or class that beans should implement/extend
     * @param includePrototypes whether to include prototype-scoped beans in the results
     * @return a list of all matching bean instances (never {@code null}, may be empty)
     */
    <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes);

    /**
     * Checks if this provider is mutable.
     *
     * <p>
     * Mutable providers allow modification of their bean registry at runtime,
     * while immutable providers have a fixed set of beans.
     * </p>
     *
     * @return {@code true} if the provider is mutable, {@code false} otherwise
     */
    boolean isMutable();

    /**
     * Queries for a single bean matching the provided reference.
     *
     * <p>
     * This method performs a precise lookup using the complete bean reference,
     * including type, name, strategy, and qualifiers. If multiple beans match,
     * the behavior is implementation-specific.
     * </p>
     *
     * @param <T> the bean type
     * @param query the bean reference to match
     * @return an {@link Optional} containing the matching bean if found, or empty if not found
     * @throws DiException if an error occurs during bean query or instantiation
     * @see BeanReference
     */
    <T> Optional<T> queryBean(BeanReference<T> query) throws DiException;

    /**
     * Queries for all beans matching the provided reference.
     *
     * <p>
     * This method retrieves all beans that satisfy the criteria specified in the
     * bean reference, allowing for flexible bean discovery and multi-bean injection.
     * </p>
     *
     * @param <T> the bean type
     * @param query the bean reference to match
     * @return a list of all matching bean instances (never {@code null}, may be empty)
     * @throws DiException if an error occurs during bean query or instantiation
     * @see BeanReference
     */
    <T> List<T> queryBeans(BeanReference<T> query) throws DiException;

    /**
     * Returns the total number of bean definitions managed by this provider.
     *
     * @return the count of bean definitions
     */
    int size();

}
