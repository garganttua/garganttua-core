package com.garganttua.core.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.context.dsl.IBeanInjectableFieldBuilder;
import com.garganttua.core.injection.context.dsl.IBeanPostConstructMethodBinderBuilder;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;
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
 *         MyService.class,
 *         Optional.of(BeanStrategy.SINGLETON),
 *         Optional.of("myService"),
 *         Set.of(Primary.class),
 *         Optional.of(constructorBinder),
 *         Set.of(postConstructBuilder),
 *         Set.of(fieldBuilder));
 *
 * // Create an example for querying
 * BeanDefinition<MyService> query = BeanDefinition.example(
 *         MyService.class,
 *         Optional.of(BeanStrategy.SINGLETON),
 *         Optional.empty(),
 *         Set.of(Primary.class));
 *
 * // Check if a definition matches the query
 * if (definition.matches(query)) {
 *     // Beans match
 * }
 * }</pre>
 *
 * @param <Bean>                            the type of bean this definition
 *                                          describes
 * @param type                              the class of the bean
 * @param strategy                          the bean strategy (scope), or empty
 *                                          for default
 * @param name                              the bean name, or empty for
 *                                          type-based naming
 * @param qualifiers                        the set of qualifier annotations
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
public record BeanDefinition<Bean>(Class<Bean> type, Optional<BeanStrategy> strategy, Optional<String> name,
        Set<Class<? extends Annotation>> qualifiers,
        Optional<IConstructorBinder<Bean>> constructorBinder,
        Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders,
        Set<IBeanInjectableFieldBuilder<?, Bean>> injectableFields) implements Dependent {

    /**
     * Returns the effective name of the bean.
     *
     * <p>
     * If a name is explicitly specified, it is returned. Otherwise, the simple
     * name of the bean type is used as the effective name.
     * </p>
     *
     * @return the effective bean name
     */
    public String effectiveName() {
        if (name.isPresent())
            return name.get();
        return type.getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BeanDefinition<?> other))
            return false;
        return Objects.equals(type, other.type) &&
                Objects.equals(strategy, other.strategy) &&
                Objects.equals(effectiveName(), other.effectiveName()) &&
                Objects.equals(qualifiers, other.qualifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, strategy, effectiveName(), qualifiers);
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "type=" + type.getName() +
                ", strategy=" + strategy +
                ", name='" + effectiveName() + '\'' +
                ", qualifiers=" + qualifiers +
                '}';
    }

    /**
     * Checks if this bean definition matches the provided query definition.
     *
     * <p>
     * This method performs a partial match where the query definition's criteria
     * are checked against this definition's properties:
     * </p>
     * <ul>
     * <li>Type: The query type must be assignable from this definition's type</li>
     * <li>Name: The effective names must match if a name is specified in the
     * query</li>
     * <li>Strategy: The strategies must match if specified in the query</li>
     * <li>Qualifiers: All query qualifiers must be present in this definition</li>
     * </ul>
     *
     * @param def the query definition to match against
     * @return {@code true} if this definition matches the query, {@code false}
     *         otherwise
     * @throws NullPointerException if def is null
     */
    public boolean matches(BeanDefinition<?> def) {
        Objects.requireNonNull(def, "BeanDefinition to match cannot be null");

        if (def.type() != null && !def.type().isAssignableFrom(this.type)) {
            return false;
        }

        if (def.name().isPresent() && !def.effectiveName().equals(this.effectiveName())) {
            return false;
        }

        if (def.strategy().isPresent() && !def.strategy().equals(this.strategy)) {
            return false;
        }

        if (def.qualifiers() != null && !def.qualifiers().isEmpty() && !this.qualifiers.containsAll(def.qualifiers())) {
            return false;
        }

        return true;
    }

    /**
     * Creates a partial bean definition example for query purposes.
     *
     * <p>
     * This factory method creates a lightweight bean definition without
     * construction
     * information (constructor binder, fields, post-construct methods). It is
     * intended
     * for use as a query criteria when searching for beans.
     * </p>
     *
     * @param <Bean>     the bean type
     * @param type       the class of the bean
     * @param strategy   the bean strategy, or empty for any strategy
     * @param name       the bean name, or empty for any name
     * @param qualifiers the required qualifier annotations
     * @return a bean definition example for querying
     */
    public static <Bean> BeanDefinition<Bean> example(Class<Bean> type, Optional<BeanStrategy> strategy,
            Optional<String> name,
            Set<Class<? extends Annotation>> qualifiers) {
        return new BeanDefinition<>(type, strategy, name, qualifiers, null, null, null);
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
    public Set<Class<?>> getDependencies() {
        Set<Class<?>> dependencies = new HashSet<>();
        this.injectableFields.stream().forEach(f -> {
            dependencies.addAll(f.getDependencies());
        });
        this.constructorBinder.ifPresent(c -> {
            dependencies.addAll(c.getDependencies());
        });
        this.postConstructMethodBinderBuilders.stream().forEach(m -> {
            dependencies.addAll(m.getDependencies());
        });
        return dependencies;
    }
}
