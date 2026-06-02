package com.garganttua.core.configuration.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method to an explicit configuration key, overriding the default
 * name-derived mapping used during builder population.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperty {

    /**
     * @return the configuration key this method is bound to
     */
    String value();
}
