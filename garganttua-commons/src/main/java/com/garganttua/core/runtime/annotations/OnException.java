package com.garganttua.core.runtime.annotations;

public @interface OnException {

    Class<? extends Throwable> exception();

    String fromStep() default "";

    String fromStage() default "";

}
