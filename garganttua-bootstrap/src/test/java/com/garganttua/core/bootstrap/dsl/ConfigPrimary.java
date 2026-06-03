package com.garganttua.core.bootstrap.dsl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/** Public test qualifier (ConfigPrimary) for verifying qualified-bean declaration from config. */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigPrimary {
}
