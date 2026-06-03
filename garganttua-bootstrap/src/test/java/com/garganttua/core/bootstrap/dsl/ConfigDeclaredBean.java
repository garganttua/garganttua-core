package com.garganttua.core.bootstrap.dsl;

/**
 * Public, instantiable bean used to verify that a bean declared purely from a configuration
 * file is registered and resolvable in the built injection context.
 */
public class ConfigDeclaredBean {

    public String hello() {
        return "config-declared";
    }
}
