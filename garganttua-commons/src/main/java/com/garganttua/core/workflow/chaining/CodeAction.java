package com.garganttua.core.workflow.chaining;

/**
 * Routing action applied when a script returns a given exit code, mapped to the
 * corresponding Garganttua Script fragment via {@link #toScript()}.
 */
public enum CodeAction {
    CONTINUE,
    ABORT,
    SKIP_STAGE,
    RETRY;

    /**
     * @return the script fragment implementing this action, or an empty string
     *         for {@link #CONTINUE} (no-op)
     */
    public String toScript() {
        return switch (this) {
            case ABORT -> "abort()";
            case SKIP_STAGE -> "skipStage()";
            case RETRY -> "retry(3, @_current_script)";
            case CONTINUE -> "";
        };
    }
}
