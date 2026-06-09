package com.garganttua.core.script.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.garganttua.core.observability.ObservabilityEmitter;
import com.garganttua.core.observability.ObservableRegistry;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeResult;
import com.garganttua.core.script.ICompiledScript;
import com.garganttua.core.script.IScriptExecutionResult;
import com.garganttua.core.script.ScriptException;

/**
 * Thread-safe {@link ICompiledScript} backed by a single immutable
 * {@link IRuntime}. Holds a reference to the {@link ScriptContext} that
 * compiled the script — needed so functions like {@code include()} /
 * {@code call()} keep working (they read the current context from
 * {@link ScriptExecutionContext}).
 *
 * <p>Mutable state on the underlying {@link ScriptContext} is NEVER touched
 * by this class: every {@link #execute(Object...)} call goes directly to
 * {@code runtime.execute(args)} and wraps the result in a fresh
 * {@link CompiledScriptExecutionResult}.
 *
 * <p><b>Re-entrancy.</b> Each {@code execute()} binds a <em>fresh</em>
 * {@link ScriptContext} frame (via {@link ScriptContext#createChildScript()})
 * as the current {@link ScriptExecutionContext}, instead of the shared
 * {@code captured} context. This is essential for re-entrant execution: the
 * built-in {@code include()} / {@code execute_script()} / {@code script_variable()}
 * functions register and resolve sub-scripts through the current context's
 * {@code includedScripts} map and read their {@code lastVariables}/{@code lastOutput}.
 * Sharing one {@code captured} context across a nested {@code execute()} of the
 * same compiled instance (e.g. a supplier resolved mid-script that re-invokes
 * the same workflow) would let the nested call's {@code include()} replace the
 * sub-script instances the enclosing call is still reading — corrupting the
 * enclosing result. A per-call frame gives each (possibly nested) execution its
 * own sub-script registry and last-* state while the immutable compiled
 * {@code runtime} stays shared.
 *
 * @since 2.0.0-ALPHA02
 */
final class CompiledScript implements ICompiledScript {

    private final IRuntime<Object[], Object> runtime;
    private final String source;
    private final ScriptContext captured;
    private final ObservableRegistry observers = new ObservableRegistry();

    CompiledScript(IRuntime<Object[], Object> runtime, String source, ScriptContext captured) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.source = Objects.requireNonNull(source, "source");
        this.captured = Objects.requireNonNull(captured, "captured ScriptContext");
    }

    @Override
    public IScriptExecutionResult execute(Object... args) throws ScriptException {
        try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.open(this.observers, UUID.randomUUID())) {
            scope.fireStart("compiledscript:execute");
            try {
                // Fresh per-call frame: its own includedScripts registry + last-* state,
                // so a re-entrant execute() of this same compiled instance cannot clobber
                // the enclosing call's sub-scripts. The immutable runtime is shared.
                ScriptContext frame = this.captured.createChildScript();
                IScriptExecutionResult result = ScriptExecutionContext.callIn(frame, () -> {
                    Optional<IRuntimeResult<Object[], Object>> raw = this.runtime.execute(args);
                    return wrap(raw);
                });
                scope.fireEnd("compiledscript:execute", result.code());
                return result;
            } catch (RuntimeException e) {
                scope.fireError("compiledscript:execute", e);
                throw e;
            }
        }
    }

    private static IScriptExecutionResult wrap(Optional<IRuntimeResult<Object[], Object>> raw) {
        if (raw.isEmpty()) {
            return new CompiledScriptExecutionResult(IRuntime.GENERIC_RUNTIME_SUCCESS_CODE,
                    Map.of(), Optional.empty(), Optional.empty(), false);
        }
        IRuntimeResult<Object[], Object> r = raw.get();
        Map<String, Object> vars = r.variables() != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(r.variables()))
                : Map.of();
        int code = r.code() != null ? r.code() : IRuntime.GENERIC_RUNTIME_SUCCESS_CODE;
        Optional<Object> output = Optional.ofNullable(r.output());
        Optional<Throwable> ex = r.getAbortingException().map(rec -> (Throwable) rec.exception());
        return new CompiledScriptExecutionResult(code, vars, output, ex, r.hasAborted());
    }

    @Override
    public String getSource() {
        return this.source;
    }
}
