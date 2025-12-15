package com.garganttua.core.expression.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

@Native
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpressionNode {

    String name() default "";

    Class<?> contextType() default Void.class;

    String description() default "";

}
