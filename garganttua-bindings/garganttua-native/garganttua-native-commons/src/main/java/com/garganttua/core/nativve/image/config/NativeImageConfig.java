package com.garganttua.core.nativve.image.config;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.io.File;
import java.io.IOException;

public class NativeImageConfig {
    private static final IDiagnostic log = Diagnostics.of(NativeImageConfig.class);

    private static final String NATIVE_IMAGE_DIR = "META-INF/native-image";
    private static final String REFLECT_CONFIG_FILE = "reflect-config.json";
    private static final String RESOURCE_CONFIG_FILE = "resource-config.json";

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