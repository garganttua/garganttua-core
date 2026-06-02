package com.garganttua.core.injection.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a class as a resolver responsible for handling a set of injection annotations.
 *
 * <p>
 * Classes annotated with {@code @Resolver} are auto-detected and associated with the
 * annotations they declare, allowing the injection framework to delegate resolution of
 * elements bearing those annotations to the resolver implementation.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@Indexed
@Reflected
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resolver {

    /**
     * The injection annotations handled by this resolver.
     *
     * @return the annotation types this resolver is responsible for
     */
    Class<? extends Annotation>[] annotations();

}
