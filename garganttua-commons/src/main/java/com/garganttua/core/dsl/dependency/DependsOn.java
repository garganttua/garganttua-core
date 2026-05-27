package com.garganttua.core.dsl.dependency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Declarative {@link DependencySpec} on a builder class. The framework
 * collects every {@code @DependsOn} declared on a builder type and merges
 * the resulting specs with whatever the builder passes manually to its
 * super-constructor.
 *
 * <h2>Defaults</h2>
 * <p>The annotation is designed so the most common case is a one-liner:
 * <pre>{@code
 * @DependsOn(target = IInjectionContextBuilder.class)
 * public final class MyBuilder extends AbstractAutomaticDependentBuilder<...> { ... }
 * }</pre>
 * which expands to {@code stage = BUILD, kind = BUILT, requirement = OPTIONAL}
 * — equivalent to {@code DependencySpec.use(IInjectionContextBuilder.class)}.
 *
 * <h2>Repeatable</h2>
 * <pre>{@code
 * @DependsOn(target = IInjectionContextBuilder.class,
 *            stage  = DependencyStage.CONFIGURATION,
 *            kind   = DependencyKind.BUILDER)
 * @DependsOn(target = IInjectionContextBuilder.class)   // BUILD + BUILT
 * public final class MyBuilder extends ... { ... }
 * }</pre>
 *
 * <p>Discovered automatically by {@link DependencySpec#fromAnnotations(Class)},
 * which the abstract dependent-builder bases call during their constructor
 * to merge with the manual dependency set.
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Repeatable(DependsOn.Set.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DependsOn {

    /** The upstream builder class this consumer depends on. */
    Class<? extends IObservableBuilder<?, ?>> target();

    /** Stage at which the dep is consumed. Defaults to {@link DependencyStage#BUILD}. */
    DependencyStage stage() default DependencyStage.BUILD;

    /** Kind requested. Defaults to {@link DependencyKind#BUILT}. */
    DependencyKind kind() default DependencyKind.BUILT;

    /** Required vs optional resolution. Defaults to {@link DependencyRequirement#OPTIONAL}. */
    DependencyRequirement requirement() default DependencyRequirement.OPTIONAL;

    /** Container for repeated {@link DependsOn} declarations. */
    @Indexed
    @Reflected
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Set {
        DependsOn[] value();
    }
}
