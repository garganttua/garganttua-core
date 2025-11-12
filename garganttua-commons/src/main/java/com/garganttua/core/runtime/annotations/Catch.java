package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.injection.DiException;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Catch {

    Class<DiException> exception();

    int code();

    boolean failback();

    boolean abort();

}
