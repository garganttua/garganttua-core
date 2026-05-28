package com.garganttua.core.script.annotations;

/**
 * Contract for classes annotated with {@link ScriptDefinition}. Implementations
 * supply the script source code via {@link #source()}; {@code ScriptsBuilder}
 * compiles and registers them under the name declared on the annotation.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IScriptDefinition {

    /** Garganttua-Script source code. Must be non-blank and well-formed. */
    String source();
}
