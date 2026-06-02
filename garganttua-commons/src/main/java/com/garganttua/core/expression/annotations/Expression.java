package com.garganttua.core.expression.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a method as an expression function for auto-detection by the expression context.
 *
 * <p>
 * Methods annotated with {@code @Expression} are automatically discovered at compile-time
 * and registered as callable functions in the expression language. This annotation
 * is also used on fields and parameters for injection of expression values.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@Indexed
@Reflected
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Expression {

    /**
     * Convenience alias for {@link #name()}; the function name exposed in the expression language.
     *
     * @return the function name, or empty to derive it from the method name
     */
    String value() default "";

    /**
     * The function name exposed in the expression language.
     *
     * @return the function name, or empty to derive it from the method name
     */
    String name() default "";

    /**
     * Human-readable description used in generated manual pages.
     *
     * @return the description, or empty if none
     */
    String description() default "";

}
