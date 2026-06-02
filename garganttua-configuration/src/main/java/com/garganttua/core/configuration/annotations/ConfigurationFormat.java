package com.garganttua.core.configuration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Declares the configuration format (e.g. {@code "json"}, {@code "yaml"}) associated
 * with the annotated type.
 */
@Indexed
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationFormat {

    /**
     * @return the format name this type is associated with
     */
    String value();
}
