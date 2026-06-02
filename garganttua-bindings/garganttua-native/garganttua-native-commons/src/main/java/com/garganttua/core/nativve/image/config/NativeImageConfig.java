package com.garganttua.core.nativve.image.config;

import com.garganttua.core.observability.Logger;
import java.io.File;
import java.io.IOException;

/**
 * Resolves the standard GraalVM native-image configuration file locations
 * (under {@code META-INF/native-image}) relative to a base directory, creating
 * the directory tree on demand.
 */
public class NativeImageConfig {
    private static final Logger log = Logger.getLogger(NativeImageConfig.class);

    private static final String NATIVE_IMAGE_DIR = "META-INF/native-image";
    private static final String REFLECT_CONFIG_FILE = "reflect-config.json";
    private static final String RESOURCE_CONFIG_FILE = "resource-config.json";

    /**
     * Resolves the {@code reflect-config.json} file under {@code baseDir}, creating
     * the {@code META-INF/native-image} directory if it does not exist.
     *
     * @param baseDir the base directory in which the native-image config tree lives
     * @return the reflection configuration file (which may not yet exist)
     * @throws IOException if the native-image directory cannot be created
     */
    public static File getReflectConfigFile(String baseDir) throws IOException {
        log.trace("Entering getReflectConfigFile with baseDir: {}", baseDir);
        File nativeImageDir = new File(baseDir, NATIVE_IMAGE_DIR);
        log.debug("Native image directory: {}", nativeImageDir);
        ensureDirectoryExists(nativeImageDir);

        File reflectConfigFile = new File(nativeImageDir, REFLECT_CONFIG_FILE);
        log.debug("Reflection config file location: {}", reflectConfigFile);
        log.trace("Exiting getReflectConfigFile");
        return reflectConfigFile;
    }

    /**
     * Resolves the {@code resource-config.json} file under {@code baseDir}, creating
     * the {@code META-INF/native-image} directory if it does not exist.
     *
     * @param baseDir the base directory in which the native-image config tree lives
     * @return the resource configuration file (which may not yet exist)
     * @throws IOException if the native-image directory cannot be created
     */
    public static File getResourceConfigFile(String baseDir) throws IOException {
        log.trace("Entering getResourceConfigFile with baseDir: {}", baseDir);
        File nativeImageDir = new File(baseDir, NATIVE_IMAGE_DIR);
        log.debug("Native image directory: {}", nativeImageDir);
        ensureDirectoryExists(nativeImageDir);

        File resourceConfigFile = new File(nativeImageDir, RESOURCE_CONFIG_FILE);
        log.debug("Resource config file location: {}", resourceConfigFile);
        log.trace("Exiting getResourceConfigFile");
        return resourceConfigFile;
    }

    private static void ensureDirectoryExists(File dir) throws IOException {
        log.trace("Ensuring directory exists: {}", dir);
        if (!dir.exists()) {
            log.debug("Creating directory: {}", dir);
            if (!dir.mkdirs()) {
                log.error("Failed to create directory: {}", dir.getAbsolutePath());
                throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
            }
            log.debug("Created native image directory: {}", dir);
        }
    }
}