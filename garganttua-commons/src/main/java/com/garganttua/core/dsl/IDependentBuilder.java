package com.garganttua.core.dsl;

import java.util.HashSet;
import java.util.Set;

import com.garganttua.core.reflection.binders.Dependent;

/**
 * Builder interface for managing dependencies on other observable builders.
 *
 * <p>
 * {@code IDependentBuilder} combines the functionality of declaring
 * dependencies
 * on other builders through both {@code use()} and {@code require()} methods.
 * The semantic distinction between these methods indicates intent:
 * </p>
 * <ul>
 * <li>{@code use()} - Declares an optional dependency that may be used if
 * available</li>
 * <li>{@code require()} - Declares a mandatory dependency that must be
 * satisfied</li>
 * </ul>
 *
 * <p>
 * This interface extends {@link Dependent} to expose the collected dependencies
 * as a set of classes, enabling dependency tracking and validation throughout
 * the builder lifecycle.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * MyBuilder builder = new MyBuilder()
 *         .use(optionalDependency) // Optional dependency
 *         .require(mandatoryDependency) // Mandatory dependency
 *         .build();
 * }</pre>
 *
 * @param <D>       the type of observable builder this builder depends on
 * @param <Builder> the concrete builder type for method chaining
 * @param <Built>   the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see IBuilder
 * @see IBuilderDependency
 * @see Dependent
 */
public interface IDependentBuilder<Builder extends IBuilder<Built>, Built>
        extends IBuilder<Built>, Dependent {

    /**
     * Declares an optional dependency on another builder.
     *
     * <p>
     * The {@code use()} method indicates that this builder may utilize the
     * specified dependency if it's available, but can function without it.
     * This is useful for optional features or enhancements.
     * </p>
     *
     * @param dependency the optional builder dependency
     * @return this builder instance for method chaining
     */
    Builder provide(IObservableBuilder<?, ?> dependency);

    Set<Class<? extends IObservableBuilder<?, ?>>> use();

    Set<Class<? extends IObservableBuilder<?, ?>>> require();

    default Set<Class<?>> dependencies() {
        Set<Class<?>> deps = new HashSet<>();
        deps.addAll(this.use());
        deps.addAll(this.require());
        return deps;
    };

}
