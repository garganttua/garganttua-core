package com.garganttua.core.dsl.dependency;

/**
 * What form a dependency is consumed in: the raw builder reference, or
 * the built object produced by that builder.
 *
 * <p>Choice of kind drives which hook the framework invokes:
 * <ul>
 *   <li>{@link #BUILDER} → {@code do<Stage>WithDependencyBuilder(IObservableBuilder)}</li>
 *   <li>{@link #BUILT}   → {@code do<Stage>WithDependency(Object)} (the legacy signature)</li>
 * </ul>
 *
 * <p>Constraint: {@link DependencyStage#CONFIGURATION} can only pair with
 * {@link #BUILDER} — at configuration time the dep is not yet built.
 *
 * @since 2.0.0-ALPHA02
 * @see DependencyStage
 * @see DependencySpec
 */
public enum DependencyKind {

    /** The consumer receives the dependency's {@code IObservableBuilder} reference. */
    BUILDER,

    /** The consumer receives the dependency's built object (post-{@code build()}). */
    BUILT;
}
