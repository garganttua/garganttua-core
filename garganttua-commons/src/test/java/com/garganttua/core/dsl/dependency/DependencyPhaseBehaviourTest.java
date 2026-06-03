package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DependencyPhaseBehaviourTest {

    @Test
    void autoDetectIncludesAutoDetectOnly() {
        assertTrue(DependencyPhase.AUTO_DETECT.includesAutoDetect());
        assertFalse(DependencyPhase.AUTO_DETECT.includesBuild());
    }

    @Test
    void buildIncludesBuildOnly() {
        assertFalse(DependencyPhase.BUILD.includesAutoDetect());
        assertTrue(DependencyPhase.BUILD.includesBuild());
    }

    @Test
    void bothIncludesEverything() {
        assertTrue(DependencyPhase.BOTH.includesAutoDetect());
        assertTrue(DependencyPhase.BOTH.includesBuild());
    }
}
