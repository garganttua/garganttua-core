package com.garganttua.core.workflow.generator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.workflow.WorkflowTimingConfig;

class ScriptGenerationOptionsBehaviourTest {

    @Test
    void nullTimingNormalisedToDisabled() {
        ScriptGenerationOptions o = new ScriptGenerationOptions(null);
        assertNotNull(o.timing());
        assertTrue(o.timing().isFullyDisabled());
    }

    @Test
    void defaultsCarryDisabledTiming() {
        ScriptGenerationOptions o = ScriptGenerationOptions.defaults();
        assertTrue(o.timing().isFullyDisabled());
    }

    @Test
    void withTimingRetainsGivenConfig() {
        WorkflowTimingConfig timing = WorkflowTimingConfig.of();
        ScriptGenerationOptions o = ScriptGenerationOptions.withTiming(timing);
        assertSame(timing, o.timing());
        assertTrue(o.timing().stagesEnabled());
    }
}
