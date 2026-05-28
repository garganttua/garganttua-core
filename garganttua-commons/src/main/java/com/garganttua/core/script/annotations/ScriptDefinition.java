package com.garganttua.core.script.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Marks a class as a declarative script definition discovered and registered
 * by {@code ScriptsBuilder} at Bootstrap time.
 *
 * <p>Annotated classes implement {@link IScriptDefinition} and provide the
 * source code via {@link IScriptDefinition#source()}. The annotation is a
 * {@code @Qualifier} so the DI container picks the class up and
 * {@code ScriptsBuilder} can resolve it via a bean query — same mechanism as
 * {@code @RuntimeDefinition} / {@code @WorkflowDefinition}.
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Qualifier
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ScriptDefinition {

    /** Logical name under which the compiled script is registered. */
    String name();
}
