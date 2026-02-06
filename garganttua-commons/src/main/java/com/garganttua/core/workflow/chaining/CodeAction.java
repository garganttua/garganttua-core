package com.garganttua.core.workflow.chaining;

public enum CodeAction {
    CONTINUE,
    ABORT,
    SKIP_STAGE,
    RETRY;

    public String toScript() {
        return switch (this) {
            case ABORT -> "abort()";
            case SKIP_STAGE -> "skipStage()";
            case RETRY -> "retry(3, @_current_script)";
            case CONTINUE -> "";
        };
    }
}
