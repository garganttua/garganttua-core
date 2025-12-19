package com.garganttua.core.expression.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * for injection resolver and Expression context auto dectection
 */
@Native
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Expression {

    String value() default "";

    String name() default "";

    String description() default "";

}
