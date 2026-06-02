package com.garganttua.core.script;

/**
 * Thread-safe, immutable handle on a script that has already been parsed,
 * compiled, and turned into an {@code IRuntime}. Sister of {@link IScript}
 * but with no mutable per-execution state — every {@link #execute(Object...)}
 * call returns its own {@link IScriptExecutionResult}, so the same instance
 * can be safely invoked concurrently from multiple threads.
 *
 * <p>Created via {@link IScriptingEnvironment#precompile(String, java.util.Map)}
 * for workflows / pipelines that execute the same script source many times
 * (per-request handlers, batch loops, etc.) — avoids the ANTLR-parse +
 * runtime-build cost on every call.
 *
 * @since 2.0.0-ALPHA02
 */
public interface ICompiledScript {

    /**
     * Execute the pre-compiled script. Thread-safe: each call uses its own
     * runtime context, no state is shared with other concurrent calls.
     *
     * @param args positional script arguments (mapped to @0, @1, …)
     * @return immutable execution result
     * @throws ScriptException if execution fails outside the script's own
     *                         error-handling (i.e. before the runtime starts)
     */
    IScriptExecutionResult execute(Object... args) throws ScriptException;

    /**
     * Returns the original script source this compiled instance was built from.
     *
     * @return the source code this compiled script came from
     */
    String getSource();
}
