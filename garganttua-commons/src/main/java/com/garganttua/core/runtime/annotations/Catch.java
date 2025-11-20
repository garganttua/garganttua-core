package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.runtime.IRuntime;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Catch {

    Class<? extends Throwable> exception();

    int code() default IRuntime.GENERIC_RUNTIME_ERROR_CODE;

}
