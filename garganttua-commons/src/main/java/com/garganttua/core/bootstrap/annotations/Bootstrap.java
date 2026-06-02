package com.garganttua.core.bootstrap.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a type as a bootstrap-discoverable builder so it is auto-detected and
 * registered during {@code Bootstrap} auto-detection.
 *
 * <p>The annotation is {@link Indexed} for compile-time discovery and
 * {@link Reflected} so it remains reflectively visible in native images.
 *
 * @since 2.0.0-ALPHA01
 */
@Indexed
@Reflected
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bootstrap {

}
