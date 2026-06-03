package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DependencyRequirementBehaviourTest {

    // --- REQUIRED ---

    @Test
    void requiredFollowsPhaseInclusion() {
        DependencyRequirement r = DependencyRequirement.REQUIRED;
        assertTrue(r.isRequiredForAutoDetect(DependencyPhase.AUTO_DETECT));
        assertTrue(r.isRequiredForAutoDetect(DependencyPhase.BOTH));
        assertFalse(r.isRequiredForAutoDetect(DependencyPhase.BUILD));

        assertTrue(r.isRequiredForBuild(DependencyPhase.BUILD));
        assertTrue(r.isRequiredForBuild(DependencyPhase.BOTH));
        assertFalse(r.isRequiredForBuild(DependencyPhase.AUTO_DETECT));
    }

    // --- OPTIONAL ---

    @Test
    void optionalNeverRequired() {
        DependencyRequirement r = DependencyRequirement.OPTIONAL;
        assertFalse(r.isRequiredForAutoDetect(DependencyPhase.BOTH));
        assertFalse(r.isRequiredForBuild(DependencyPhase.BOTH));
    }

    @Test
    void optionalIsOptionalWhenPhaseIncludesIt() {
        DependencyRequirement r = DependencyRequirement.OPTIONAL;
        assertTrue(r.isOptionalForAutoDetect(DependencyPhase.AUTO_DETECT));
        assertTrue(r.isOptionalForBuild(DependencyPhase.BUILD));
        assertFalse(r.isOptionalForAutoDetect(DependencyPhase.BUILD));
        assertFalse(r.isOptionalForBuild(DependencyPhase.AUTO_DETECT));
    }

    // --- REQUIRED_FOR_AUTO_DETECT ---

    @Test
    void requiredForAutoDetectVariant() {
        DependencyRequirement r = DependencyRequirement.REQUIRED_FOR_AUTO_DETECT;
        assertTrue(r.isRequiredForAutoDetect(DependencyPhase.BOTH));
        assertTrue(r.isRequiredForAutoDetect(DependencyPhase.AUTO_DETECT));
        assertFalse(r.isRequiredForAutoDetect(DependencyPhase.BUILD));
        // not required for build
        assertFalse(r.isRequiredForBuild(DependencyPhase.BOTH));
        // therefore optional for build when build is included
        assertTrue(r.isOptionalForBuild(DependencyPhase.BOTH));
    }

    // --- REQUIRED_FOR_BUILD ---

    @Test
    void requiredForBuildVariant() {
        DependencyRequirement r = DependencyRequirement.REQUIRED_FOR_BUILD;
        assertTrue(r.isRequiredForBuild(DependencyPhase.BOTH));
        assertTrue(r.isRequiredForBuild(DependencyPhase.BUILD));
        assertFalse(r.isRequiredForBuild(DependencyPhase.AUTO_DETECT));
        assertFalse(r.isRequiredForAutoDetect(DependencyPhase.BOTH));
        assertTrue(r.isOptionalForAutoDetect(DependencyPhase.BOTH));
    }

    // --- optional is always !required when phase includes the phase ---

    @Test
    void optionalIsInverseOfRequiredWithinIncludedPhase() {
        for (DependencyRequirement r : DependencyRequirement.values()) {
            assertTrue(r.isRequiredForBuild(DependencyPhase.BOTH)
                    != r.isOptionalForBuild(DependencyPhase.BOTH));
            assertTrue(r.isRequiredForAutoDetect(DependencyPhase.BOTH)
                    != r.isOptionalForAutoDetect(DependencyPhase.BOTH));
        }
    }

    @Test
    void notIncludedPhaseIsNeitherRequiredNorOptional() {
        DependencyRequirement r = DependencyRequirement.REQUIRED;
        // BUILD phase does not include auto-detect
        assertFalse(r.isRequiredForAutoDetect(DependencyPhase.BUILD));
        assertFalse(r.isOptionalForAutoDetect(DependencyPhase.BUILD));
    }
}
