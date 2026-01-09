package com.garganttua.core.bootstrap.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

@Native
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bootstrap {

}
