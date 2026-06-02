package com.garganttua.core.dsl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Declares a base package that a builder should scan for components during
 * auto-detection.
 *
 * <p>The annotation is {@link Indexed} and {@link Reflected} so it is
 * discovered at compile time and remains visible to reflection at runtime.
 *
 * @since 2.0.0-ALPHA01
 */
@Indexed
@Reflected
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scan {

    /**
     * The base package to scan for components.
     *
     * @return the fully-qualified base package name
     */
    String scan();

}
