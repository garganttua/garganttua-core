package com.garganttua.core.workflow.chaining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodeActionBehaviourTest {

    @Test
    void abortScriptFragment() {
        assertEquals("abort()", CodeAction.ABORT.toScript());
    }

    @Test
    void skipStageScriptFragment() {
        assertEquals("skipStage()", CodeAction.SKIP_STAGE.toScript());
    }

    @Test
    void retryScriptFragment() {
        assertEquals("retry(3, @_current_script)", CodeAction.RETRY.toScript());
    }

    @Test
    void continueIsNoOpEmptyFragment() {
        assertTrue(CodeAction.CONTINUE.toScript().isEmpty());
    }
}
