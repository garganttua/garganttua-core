package com.garganttua.core.script.context;

import java.util.Map;
import java.util.Optional;

import com.garganttua.core.script.IScriptExecutionResult;

/**
 * Immutable {@link IScriptExecutionResult} record-style value object returned
 * by {@link CompiledScript#execute(Object...)}.
 *
 * @since 2.0.0-ALPHA02
 */
record CompiledScriptExecutionResult(
        int code,
        Map<String, Object> variables,
        Optional<Object> output,
        Optional<Throwable> exception,
        boolean hasAborted) implements IScriptExecutionResult {
}
