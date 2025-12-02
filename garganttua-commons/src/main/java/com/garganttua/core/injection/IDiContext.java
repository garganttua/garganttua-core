package com.garganttua.core.injection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.nativve.INativeConfiguration;
import com.garganttua.core.utils.Copyable;

/**
 * Central interface for the dependency injection context managing beans, properties, and child contexts.
 *
 * <p>
 * {@code IDiContext} serves as the main entry point for dependency injection operations. It manages
 * multiple bean providers and property providers, enables bean and property queries, and supports
 * hierarchical context creation through child contexts. The context also integrates with the
 * element resolution system for automatic dependency injection.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Query beans from the context
 * IDiContext context = ...;
 *
 * // Query a bean by definition
 * BeanDefinition<MyService> definition = BeanDefinition.example(
 *     MyService.class,
 *     Optional.of(BeanStrategy.SINGLETON),
 *     Optional.of("myService"),
 *     Set.of());
 * Optional<MyService> service = context.queryBean(definition);
 *
 * // Get properties
 * Optional<String> dbUrl = context.getProperty("db.url", String.class);
 *
 * // Create a child context
 * IDiContext childContext = context.newChildContext(IDiContext.class);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the context implementation and whether it is mutable.
 * Typically, immutable contexts are thread-safe while mutable contexts require synchronization.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IBeanProvider
 * @see IPropertyProvider
 * @see IInjectableElementResolver
 * @see ILifecycle
 */
public interface IDiContext extends ILifecycle, IInjectableElementResolver, Copyable<IDiContext>, INativeConfiguration {

        // --- Bean Scopes ---

        /**
         * Returns all bean providers registered in this context.
         *
         * @return a set of all bean providers (never {@code null})
         * @throws DiException if an error occurs while retrieving providers
         */
        Set<IBeanProvider> getBeanProviders() throws DiException;

        /**
         * Retrieves a specific bean provider by name.
         *
         * @param name the name of the provider
         * @return an {@link Optional} containing the provider if found, or empty otherwise
         */
        Optional<IBeanProvider> getBeanProvider(String name);

        /**
         * Queries for a single bean matching the definition from a specific provider.
         *
         * @param <Bean> the bean type
         * @param provider the provider name (empty to search all providers)
         * @param definition the bean definition to match
         * @return an {@link Optional} containing the bean if found, or empty otherwise
         * @throws DiException if an error occurs during query or bean instantiation
         */
        <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanDefinition<Bean> definition) throws DiException;

        /**
         * Queries for a single bean matching the definition from all providers.
         *
         * @param <Bean> the bean type
         * @param definition the bean definition to match
         * @return an {@link Optional} containing the bean if found, or empty otherwise
         * @throws DiException if an error occurs during query or bean instantiation
         */
        <Bean> Optional<Bean> queryBean(BeanDefinition<Bean> definition) throws DiException;

        /**
         * Queries for a single bean matching the definition from a named provider.
         *
         * @param <Bean> the bean type
         * @param provider the provider name
         * @param definition the bean definition to match
         * @return an {@link Optional} containing the bean if found, or empty otherwise
         * @throws DiException if an error occurs during query or bean instantiation
         */
        <Bean> Optional<Bean> queryBean(String provider, BeanDefinition<Bean> definition) throws DiException;

        /**
         * Queries for all beans matching the definition from a specific provider.
         *
         * @param <Bean> the bean type
         * @param provider the provider name (empty to search all providers)
         * @param definition the bean definition to match
         * @return a list of all matching beans (never {@code null}, may be empty)
         * @throws DiException if an error occurs during query or bean instantiation
         */
        <Bean> List<Bean> queryBeans(Optional<String> provider, BeanDefinition<Bean> definition) throws DiException;

        /**
         * Queries for all beans matching the definition from all providers.
         *
         * @param <Bean> the bean type
         * @param definition the bean definition to match
         * @return a list of all matching beans (never {@code null}, may be empty)
         * @throws DiException if an error occurs during query or bean instantiation
         */
        <Bean> List<Bean> queryBeans(BeanDefinition<Bean> definition) throws DiException;

        /**
         * Queries for all beans matching the definition from a named provider.
         *
         * @param <Bean> the bean type
         * @param provider the provider name
         * @param definition the bean definition to match
         * @return a list of all matching beans (never {@code null}, may be empty)
         * @throws DiException if an error occurs during query or bean instantiation
         */
        <Bean> List<Bean> queryBeans(String provider, BeanDefinition<Bean> definition) throws DiException;

        // --- Property Scopes ---

        /**
         * Returns all property providers registered in this context.
         *
         * @return a set of all property providers (never {@code null})
         * @throws DiException if an error occurs while retrieving providers
         */
        Set<IPropertyProvider> getPropertyProviders() throws DiException;

        /**
         * Retrieves a specific property provider by name.
         *
         * @param name the name of the provider
         * @return an {@link Optional} containing the provider if found, or empty otherwise
         */
        Optional<IPropertyProvider> getPropertyProvider(String name);

        /**
         * Retrieves a property value from a specific provider.
         *
         * @param <T> the property type
         * @param provider the provider name (empty to search all providers)
         * @param key the property key
         * @param type the expected property type
         * @return an {@link Optional} containing the property value if found, or empty otherwise
         * @throws DiException if an error occurs during property retrieval or type conversion
         */
        <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException;

        /**
         * Retrieves a property value from all providers.
         *
         * @param <T> the property type
         * @param key the property key
         * @param type the expected property type
         * @return an {@link Optional} containing the property value if found, or empty otherwise
         * @throws DiException if an error occurs during property retrieval or type conversion
         */
        <T> Optional<T> getProperty(String key, Class<T> type) throws DiException;

        /**
         * Retrieves a property value from a named provider.
         *
         * @param <T> the property type
         * @param providerName the provider name
         * @param key the property key
         * @param type the expected property type
         * @return an {@link Optional} containing the property value if found, or empty otherwise
         * @throws DiException if an error occurs during property retrieval or type conversion
         */
        <T> Optional<T> getProperty(String providerName, String key, Class<T> type) throws DiException;

        /**
         * Sets a property value in the specified provider.
         *
         * @param provider the provider name
         * @param key the property key
         * @param value the property value
         * @throws DiException if the provider is immutable or an error occurs
         */
        void setProperty(String provider, String key, Object value) throws DiException;

        // --- Core ---

        /**
         * Creates a new child context with the specified type and arguments.
         *
         * <p>
         * The child context is created by cloning this context and then using a registered
         * factory to instantiate the child. The child context inherits beans and properties
         * from the parent but can have its own modifications.
         * </p>
         *
         * @param <ChildContext> the child context type
         * @param contextClass the class of the child context to create
         * @param args additional arguments for child context initialization
         * @return the created child context
         * @throws DiException if no factory is registered or child creation fails
         */
        <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass, Object... args)
                        throws DiException;

        /**
         * Registers a factory for creating child contexts.
         *
         * @param factory the child context factory to register
         */
        void registerChildContextFactory(IDiChildContextFactory<? extends IDiContext> factory);

        /**
         * Returns all registered child context factories.
         *
         * @param <ChildContext> the child context type
         * @return a set of all child context factories (never {@code null})
         * @throws DiException if an error occurs while retrieving factories
         */
        <ChildContext extends IDiContext> Set<IDiChildContextFactory<ChildContext>> getChildContextFactories()
                        throws DiException;

}
