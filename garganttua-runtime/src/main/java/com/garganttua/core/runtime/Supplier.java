package com.garganttua.core.runtime;

public @interface Supplier {

    String name() default "";

    boolean contextual() default false;

    Class<?> contextType() default Object.class;

    String description() default "";

}
