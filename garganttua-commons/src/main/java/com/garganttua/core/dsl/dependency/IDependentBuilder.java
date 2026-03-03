package com.garganttua.core.dsl.dependency;

import java.util.HashSet;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

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
 * This interface exposes the collected dependencies as a set of classes,
 * enabling dependency tracking and validation throughout the builder lifecycle.
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
 * @see DependencySpec
 */
public interface IDependentBuilder<Builder extends IBuilder<Built>, Built>
        extends IBuilder<Built> {

    /**
     * Provides a dependency to this builder.
     *
     * <p>
     * The {@code provide()} method supplies a concrete instance of a declared
     * dependency to this builder. The dependency must have been declared via
     * the builder's dependency specifications.
     * </p>
     *
     * @param dependency the builder dependency to provide
     * @return this builder instance for method chaining
     * @throws DslException if the dependency is not in the expected dependencies list
     */
    Builder provide(IObservableBuilder<?, ?> dependency) throws DslException;

    Set<Class<? extends IObservableBuilder<?, ?>>> use();

    Set<Class<? extends IObservableBuilder<?, ?>>> require();

    default Set<IClass<?>> dependencies() {
        Set<IClass<?>> deps = new HashSet<>();
        return deps;
    }

}
