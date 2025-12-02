package com.garganttua.core.nativve.image.config;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NativeImageConfig {

    private static final String NATIVE_IMAGE_DIR = "META-INF/native-image";
    private static final String REFLECT_CONFIG_FILE = "reflect-config.json";
    private static final String RESOURCE_CONFIG_FILE = "resource-config.json";

    public static File getReflectConfigFile(String baseDir) throws IOException {
        log.atTrace().log("Entering getReflectConfigFile with baseDir: {}", baseDir);
        File nativeImageDir = new File(baseDir, NATIVE_IMAGE_DIR);
        log.atDebug().log("Native image directory: {}", nativeImageDir);
        ensureDirectoryExists(nativeImageDir);

        File reflectConfigFile = new File(nativeImageDir, REFLECT_CONFIG_FILE);
        log.atDebug().log("Reflection config file location: {}", reflectConfigFile);
        log.atTrace().log("Exiting getReflectConfigFile");
        return reflectConfigFile;
    }

    public static File getResourceConfigFile(String baseDir) throws IOException {
        log.atTrace().log("Entering getResourceConfigFile with baseDir: {}", baseDir);
        File nativeImageDir = new File(baseDir, NATIVE_IMAGE_DIR);
        log.atDebug().log("Native image directory: {}", nativeImageDir);
        ensureDirectoryExists(nativeImageDir);

        File resourceConfigFile = new File(nativeImageDir, RESOURCE_CONFIG_FILE);
        log.atDebug().log("Resource config file location: {}", resourceConfigFile);
        log.atTrace().log("Exiting getResourceConfigFile");
        return resourceConfigFile;
    }

    private static void ensureDirectoryExists(File dir) throws IOException {
        log.atTrace().log("Ensuring directory exists: {}", dir);
        if (!dir.exists()) {
            log.atDebug().log("Creating directory: {}", dir);
            if (!dir.mkdirs()) {
                log.atError().log("Failed to create directory: {}", dir.getAbsolutePath());
                throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
            }
            log.atInfo().log("Created native image directory: {}", dir);
        }
    }
}