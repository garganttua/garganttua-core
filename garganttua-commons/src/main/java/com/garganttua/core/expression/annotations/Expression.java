package com.garganttua.core.expression.annotations;

public @interface Expression {

    String name() default "";

    Class<?> contextType() default Void.class;

    String description() default "";

}
