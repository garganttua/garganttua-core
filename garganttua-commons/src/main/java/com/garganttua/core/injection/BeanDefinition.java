package com.garganttua.core.injection;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.context.dsl.IBeanInjectableFieldBuilder;
import com.garganttua.core.injection.context.dsl.IBeanPostConstructMethodBinderBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.IConstructorBinder;

/**
 * Immutable definition of a bean including its metadata and construction
 * information.
 *
 * <p>
 * {@code BeanDefinition} encapsulates all the information required to identify,
 * match,
 * and instantiate a bean within the dependency injection system. It includes
 * the bean's
 * type, strategy (scope), name, qualifier annotations, constructor binder,
 * post-construct
 * methods, and injectable fields. This record serves as both a complete bean
 * blueprint
 * and a query criteria for bean lookup operations.
 * </p>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create a complete bean definition
 * BeanDefinition<MyService> definition = new BeanDefinition<>(
 *         reference,
 *         Optional.of(constructorBinder),
 *         Set.of(postConstructBuilder),
 *         Set.of(fieldBuilder));
 *
 * // Match its reference against a query reference
 * if (definition.reference().matches(query)) {
 *     // Beans match
 * }
 *
 * // Collect all dependency types for resolution ordering
 * Set<IClass<?>> deps = definition.dependencies();
 * }</pre>
 *
 * @param <Bean>                            the type of bean this definition
 *                                          describes
 * @param reference                         the bean reference containing type,
 *                                          strategy, name, and qualifiers
 * @param constructorBinder                 the constructor binder for bean
 *                                          instantiation
 * @param postConstructMethodBinderBuilders the set of post-construct method
 *                                          builders
 * @param injectableFields                  the set of injectable field builders
 * @since 2.0.0-ALPHA01
 * @see BeanStrategy
 * @see IBeanFactory
 * @see Dependent
 */
public record BeanDefinition<Bean>(BeanReference<Bean> reference,
        Optional<IConstructorBinder<Bean>> constructorBinder,
        Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders,
        Set<IBeanInjectableFieldBuilder<?, Bean>> injectableFields) implements Dependent {

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BeanDefinition<?> other))
            return false;
        return Objects.equals(reference, other.reference);
    }

    @Override
    public int hashCode() {
        return reference.hashCode();
    }

    @Override
    public String toString() {
        return reference.toString();
    }
    /**
     * Returns all dependency types required by this bean definition.
     *
     * <p>
     * This method collects all dependencies from the constructor parameters,
     * injectable fields, and post-construct method parameters. The returned
     * set is used for dependency resolution order and circular dependency
     * detection.
     * </p>
     *
     * @return a set of all dependency classes (never {@code null})
     */
    public Set<IClass<?>> dependencies() {
        Set<IClass<?>> dependencies = new HashSet<>();
        this.injectableFields.stream().forEach(f -> {
            dependencies.addAll(f.dependencies());
        });
        this.constructorBinder.ifPresent(c -> {
            dependencies.addAll(c.dependencies());
        });
        this.postConstructMethodBinderBuilders.stream().forEach(m -> {
            dependencies.addAll(m.dependencies());
        });
        return dependencies;
    }
}
