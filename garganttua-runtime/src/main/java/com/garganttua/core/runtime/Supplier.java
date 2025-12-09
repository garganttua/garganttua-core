package com.garganttua.core.runtime;

public @interface Supplier {

    String name() default "";

    Class<?> contextType() default Object.class;

    String description() default "";

}
