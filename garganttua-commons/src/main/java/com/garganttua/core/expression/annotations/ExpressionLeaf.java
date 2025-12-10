package com.garganttua.core.expression.annotations;

public @interface ExpressionLeaf {

    String name() default "";

    Class<?> contextType() default Void.class;

    String description() default "";

}
