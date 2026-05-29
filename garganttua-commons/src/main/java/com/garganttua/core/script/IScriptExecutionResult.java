package com.garganttua.core.script;

import java.util.Map;
import java.util.Optional;

/**
 * Per-call result of {@link ICompiledScript#execute(Object...)}. Immutable
 * snapshot of what a single script execution produced — variables, output,
 * exit code, and any exception. Returned fresh from every call so concurrent
 * executions on the same {@link ICompiledScript} don't share state.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IScriptExecutionResult {

    /** Exit code from the script — 0 is success, non-zero is failure code. */
    int code();

    /** Snapshot of the script's named variables after execution. */
    Map<String, Object> variables();

    /** The script's last output (last evaluated expression), if any. */
    Optional<Object> output();

    /** Exception thrown during execution, if any. */
    Optional<Throwable> exception();

    /** True if the script aborted (vs. ran to completion). */
    boolean hasAborted();
}
