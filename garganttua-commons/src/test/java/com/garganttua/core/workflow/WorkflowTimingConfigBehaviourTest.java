package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorkflowTimingConfigBehaviourTest {

    @Test
    void disabledEmitsNothingAndIsSingleton() {
        WorkflowTimingConfig d = WorkflowTimingConfig.disabled();
        assertFalse(d.stagesEnabled());
        assertFalse(d.scriptsEnabled());
        assertTrue(d.isFullyDisabled());
        assertSame(d, WorkflowTimingConfig.disabled());
    }

    @Test
    void ofEnablesBothScopes() {
        WorkflowTimingConfig c = WorkflowTimingConfig.of();
        assertTrue(c.stagesEnabled());
        assertTrue(c.scriptsEnabled());
        assertFalse(c.isFullyDisabled());
        assertTrue(c.isStageEnabled("any"));
        assertTrue(c.isScriptEnabled("s", "x"));
    }

    @Test
    void scopeTogglesAreImmutableCopies() {
        WorkflowTimingConfig base = WorkflowTimingConfig.of();
        WorkflowTimingConfig noStages = base.stages(false);
        // original unchanged
        assertTrue(base.stagesEnabled());
        assertFalse(noStages.stagesEnabled());
        assertTrue(noStages.scriptsEnabled());

        WorkflowTimingConfig noScripts = base.scripts(false);
        assertFalse(noScripts.scriptsEnabled());
        assertTrue(noScripts.stagesEnabled());
    }

    @Test
    void fullyDisabledOnlyWhenBothOff() {
        assertFalse(WorkflowTimingConfig.of().stages(false).isFullyDisabled());
        assertTrue(WorkflowTimingConfig.of().stages(false).scripts(false).isFullyDisabled());
    }

    @Test
    void disableStageExcludesNamedStageButNotOthers() {
        WorkflowTimingConfig c = WorkflowTimingConfig.of().disableStage("validation");
        assertFalse(c.isStageEnabled("validation"));
        assertTrue(c.isStageEnabled("processing"));
        // global still enabled
        assertTrue(c.stagesEnabled());
    }

    @Test
    void disableScriptUsesStageDotScriptKey() {
        WorkflowTimingConfig c = WorkflowTimingConfig.of().disableScript("stageA.scriptB");
        assertFalse(c.isScriptEnabled("stageA", "scriptB"));
        assertTrue(c.isScriptEnabled("stageA", "other"));
        assertTrue(c.isScriptEnabled("stageB", "scriptB"));
    }

    @Test
    void globalDisableTrumpsPerNameChecks() {
        WorkflowTimingConfig c = WorkflowTimingConfig.of().stages(false);
        assertFalse(c.isStageEnabled("anything"));
    }
}
