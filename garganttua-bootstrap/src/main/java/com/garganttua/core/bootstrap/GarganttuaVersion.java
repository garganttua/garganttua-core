package com.garganttua.core.bootstrap;

import com.garganttua.core.observability.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides version information for Garganttua Core.
 *
 * <p>
 * This class reads version information from the {@code garganttua-version.properties}
 * file that is filtered by Maven during build. The version is injected automatically
 * from the Maven POM.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public final class GarganttuaVersion {
    private static final Logger log = Logger.getLogger(GarganttuaVersion.class);

    private static final String PROPERTIES_FILE = "garganttua-version.properties";
    private static final String UNKNOWN = "UNKNOWN";

    private static final Properties PROPERTIES = new Properties();
    private static boolean loaded = false;

    static {
        loadProperties();
    }

    private GarganttuaVersion() {
        // Utility class
    }

    /**
     * Loads the version properties from the classpath.
     */
    private static synchronized void loadProperties() {
        if (loaded) {
            return;
        }

        try (InputStream is = GarganttuaVersion.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) {
                PROPERTIES.load(is);
                log.debug("Loaded Garganttua version properties: {}", PROPERTIES);
            } else {
                log.warn("Garganttua version properties file not found: {}", PROPERTIES_FILE);
            }
        } catch (IOException e) {
            log.warn("Failed to load Garganttua version properties", e);
        }

        loaded = true;
    }

    /**
     * Returns the Garganttua Core version.
     *
     * @return the version string (e.g., "2.0.0-ALPHA02")
     */
    public static String getVersion() {
        return PROPERTIES.getProperty("garganttua.version", UNKNOWN);
    }

    /**
     * Returns the Garganttua Core name.
     *
     * @return the name (e.g., "Garganttua Core")
     */
    public static String getName() {
        return PROPERTIES.getProperty("garganttua.name", "Garganttua");
    }

    /**
     * Returns the Maven group ID.
     *
     * @return the group ID (e.g., "com.garganttua.core")
     */
    public static String getGroupId() {
        return PROPERTIES.getProperty("garganttua.groupId", UNKNOWN);
    }

    /**
     * Returns the Maven artifact ID.
     *
     * @return the artifact ID (e.g., "garganttua-bootstrap")
     */
    public static String getArtifactId() {
        return PROPERTIES.getProperty("garganttua.artifactId", UNKNOWN);
    }

    /**
     * Returns the full version string with name.
     *
     * @return formatted string like "Garganttua Core v2.0.0-ALPHA02"
     */
    public static String getFullVersion() {
        return getName() + " v" + getVersion();
    }

    /**
     * Checks if the version is a development/snapshot version.
     *
     * @return true if this is a snapshot or alpha/beta version
     */
    public static boolean isDevelopment() {
        String version = getVersion();
        return version.contains("SNAPSHOT") ||
               version.contains("ALPHA") ||
               version.contains("BETA") ||
               version.contains("RC");
    }
}
