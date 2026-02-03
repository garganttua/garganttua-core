package com.garganttua.core.script.loader;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for reading Garganttua-specific attributes from JAR manifests.
 *
 * <p>
 * This class reads the {@code Garganttua-Packages} attribute from a JAR's manifest
 * to determine which packages should be scanned for components (expressions, beans, etc.)
 * when the JAR is dynamically loaded at runtime.
 * </p>
 *
 * <h2>Manifest Format</h2>
 * <pre>
 * Manifest-Version: 1.0
 * Garganttua-Packages: com.myplugin,com.myplugin.expressions
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * URL jarUrl = new File("plugin.jar").toURI().toURL();
 * List<String> packages = JarManifestReader.getPackages(jarUrl);
 * for (String pkg : packages) {
 *     bootstrap.withPackage(pkg);
 * }
 * bootstrap.rebuild();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public final class JarManifestReader {

    /**
     * The manifest attribute name for declaring packages to scan.
     */
    public static final String PACKAGES_ATTRIBUTE = "Garganttua-Packages";

    private JarManifestReader() {
        // Utility class
    }

    /**
     * Reads the packages to scan from the JAR's manifest.
     *
     * <p>
     * This method reads the {@link #PACKAGES_ATTRIBUTE} attribute from the JAR's
     * manifest file. The attribute value should be a comma-separated list of
     * package names.
     * </p>
     *
     * @param jarUrl the URL of the JAR file
     * @return a list of package names to scan, or an empty list if the attribute
     *         is not present or the manifest cannot be read
     */
    public static List<String> getPackages(URL jarUrl) {
        log.atTrace().log("Entering getPackages(jarUrl={})", jarUrl);

        if (jarUrl == null) {
            log.atWarn().log("JAR URL is null, returning empty list");
            return List.of();
        }

        try {
            File jarFile = new File(jarUrl.toURI());
            if (!jarFile.exists()) {
                log.atWarn().log("JAR file does not exist: {}", jarUrl);
                return List.of();
            }

            try (JarFile jar = new JarFile(jarFile)) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) {
                    log.atDebug().log("No manifest found in JAR: {}", jarUrl);
                    return List.of();
                }

                String packages = manifest.getMainAttributes().getValue(PACKAGES_ATTRIBUTE);
                if (packages == null || packages.isBlank()) {
                    log.atDebug().log("No {} attribute found in JAR manifest: {}", PACKAGES_ATTRIBUTE, jarUrl);
                    return List.of();
                }

                List<String> result = Arrays.stream(packages.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                log.atDebug().log("Found {} packages in JAR manifest {}: {}", result.size(), jarUrl, result);
                log.atTrace().log("Exiting getPackages()");
                return result;
            }
        } catch (Exception e) {
            log.atWarn().log("Failed to read manifest from JAR: {}", jarUrl, e);
            return List.of();
        }
    }

    /**
     * Checks if a JAR file has the Garganttua-Packages manifest attribute.
     *
     * @param jarUrl the URL of the JAR file
     * @return {@code true} if the JAR has the Garganttua-Packages attribute,
     *         {@code false} otherwise
     */
    public static boolean hasGarganttuaPackages(URL jarUrl) {
        return !getPackages(jarUrl).isEmpty();
    }
}
