package com.garganttua.core.injection.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

@Indexed
@Native
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resolver {

    Class<? extends Annotation>[] annotations();

}
