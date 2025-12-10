package com.garganttua.core.expression.annotations;

public @interface ExpressionNode {

    String name() default "";

    Class<?> contextType() default Void.class;

    String description() default "";

}
