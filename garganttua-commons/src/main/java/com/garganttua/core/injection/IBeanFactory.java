package com.garganttua.core.injection;

import java.util.Set;

import com.garganttua.core.nativve.INativeElement;

/**
 * Represents a factory responsible for creating and managing bean instances in the dependency injection system.
 *
 * <p>
 * An {@code IBeanFactory} encapsulates the logic for instantiating beans based on their
 * {@link BeanDefinition}. It extends {@link IBeanSupplier} to provide the actual bean instances
 * and manages the bean's metadata, dependencies, and matching criteria. This interface is central
 * to the bean lifecycle management and dependency resolution process.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a bean factory for a specific type
 * IBeanFactory<MyService> factory = ...;
 *
 * // Get the bean definition
 * BeanDefinition<MyService> definition = factory.getDefinition();
 *
 * // Check if the factory matches a query
 * BeanDefinition<?> query = BeanDefinition.example(MyService.class,
 *     Optional.of(BeanStrategy.SINGLETON),
 *     Optional.of("myService"),
 *     Set.of());
 * if (factory.matches(query)) {
 *     MyService instance = factory.supply(context);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementation-specific. Refer to concrete implementations for thread safety guarantees.
 * </p>
 *
 * @param <Bean> the type of bean this factory produces
 * @since 2.0.0-ALPHA01
 * @see IBeanSupplier
 * @see BeanDefinition
 * @see BeanStrategy
 */
public interface IBeanFactory<Bean> extends IBeanSupplier<Bean>, INativeElement {

    /**
     * Checks if this factory's bean definition matches the provided example definition.
     *
     * <p>
     * This method determines if the bean produced by this factory satisfies the criteria
     * specified in the example definition, including type compatibility, name, strategy,
     * and qualifiers. This is used for bean query and resolution operations.
     * </p>
     *
     * @param example the bean definition to match against
     * @return {@code true} if this factory matches the example, {@code false} otherwise
     * @see BeanDefinition#matches(BeanDefinition)
     */
    boolean matches(BeanDefinition<?> example);

    /**
     * Returns the complete bean definition managed by this factory.
     *
     * <p>
     * The definition contains all metadata about the bean including its type, strategy,
     * name, qualifiers, constructor binder, post-construct methods, and injectable fields.
     * </p>
     *
     * @return the bean definition for this factory
     */
    BeanDefinition<Bean> getDefinition();

    /**
     * Returns the set of dependency types required to instantiate the bean.
     *
     * <p>
     * This method analyzes the bean's constructor parameters, injectable fields, and
     * post-construct method parameters to determine all required dependencies. This
     * information is used for dependency resolution order and circular dependency detection.
     * </p>
     *
     * @return a set of classes representing the bean's dependencies
     */
    Set<Class<?>> getDependencies();

}
