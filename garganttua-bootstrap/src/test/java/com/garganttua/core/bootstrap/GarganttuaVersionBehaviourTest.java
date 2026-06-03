package com.garganttua.core.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link GarganttuaVersion}.
 *
 * <p>The {@code garganttua-version.properties} resource is Maven-filtered at build
 * time, so the values asserted here are exactly the filtered values that end up on
 * the test classpath (target/classes). These are load-bearing for the build.
 */
@DisplayName("GarganttuaVersion behaviour")
class GarganttuaVersionBehaviourTest {

    @Test
    @DisplayName("getVersion returns the Maven-filtered project version")
    void getVersionReturnsFilteredVersion() {
        // The build filters ${project.version} -> the real version.
        // It must never be the UNKNOWN fallback because the resource exists.
        String version = GarganttuaVersion.getVersion();
        assertFalse(version.isBlank(), "version must not be blank");
        assertFalse("UNKNOWN".equals(version),
                "version property is present so it must not fall back to UNKNOWN");
        // It is a real ALPHA02 dotted version in this module.
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"),
                "version should look like a dotted release, was: " + version);
    }

    @Test
    @DisplayName("getName returns the configured product name")
    void getNameReturnsConfiguredName() {
        assertEquals("Garganttua Core", GarganttuaVersion.getName());
    }

    @Test
    @DisplayName("getGroupId returns the Maven-filtered groupId")
    void getGroupIdReturnsFilteredGroupId() {
        assertEquals("com.garganttua.core", GarganttuaVersion.getGroupId());
    }

    @Test
    @DisplayName("getArtifactId returns the Maven-filtered artifactId")
    void getArtifactIdReturnsFilteredArtifactId() {
        assertEquals("garganttua-bootstrap", GarganttuaVersion.getArtifactId());
    }

    @Test
    @DisplayName("getFullVersion concatenates name and version with a 'v' prefix")
    void getFullVersionConcatenatesNameAndVersion() {
        String expected = GarganttuaVersion.getName() + " v" + GarganttuaVersion.getVersion();
        assertEquals(expected, GarganttuaVersion.getFullVersion());
        assertTrue(GarganttuaVersion.getFullVersion().startsWith("Garganttua Core v"));
    }

    @Test
    @DisplayName("isDevelopment is true for an ALPHA/BETA/SNAPSHOT/RC version")
    void isDevelopmentReflectsVersionString() {
        String version = GarganttuaVersion.getVersion();
        boolean expectedDev = version.contains("SNAPSHOT")
                || version.contains("ALPHA")
                || version.contains("BETA")
                || version.contains("RC");
        assertEquals(expectedDev, GarganttuaVersion.isDevelopment(),
                "isDevelopment must agree with the markers present in: " + version);
    }

    @Test
    @DisplayName("repeated getters are stable (properties loaded once, statically cached)")
    void gettersAreStableAcrossCalls() {
        assertEquals(GarganttuaVersion.getVersion(), GarganttuaVersion.getVersion());
        assertEquals(GarganttuaVersion.getGroupId(), GarganttuaVersion.getGroupId());
        assertEquals(GarganttuaVersion.getArtifactId(), GarganttuaVersion.getArtifactId());
    }
}
