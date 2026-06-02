package com.garganttua.core.configuration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a type as populatable from configuration, optionally scoping its keys
 * under a common {@link #prefix()}.
 */
@Indexed
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Configurable {

    /**
     * @return the key prefix applied to all properties of this type; empty for no prefix
     */
    String prefix() default "";
}
