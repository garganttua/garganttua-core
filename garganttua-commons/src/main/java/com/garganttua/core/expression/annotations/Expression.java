package com.garganttua.core.expression.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
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
@Native
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Expression {

    String value() default "";

    String name() default "";

    String description() default "";

}
