package com.garganttua.nativve.image.config;

import java.io.File;
import java.io.IOException;

public class NativeImageConfig {

    private static final String NATIVE_IMAGE_DIR = "META-INF/native-image";
    private static final String REFLECT_CONFIG_FILE = "reflect-config.json";
    private static final String RESOURCE_CONFIG_FILE = "resource-config.json";

    public static File getReflectConfigFile(String baseDir) throws IOException {
        File nativeImageDir = new File(baseDir, NATIVE_IMAGE_DIR);
        ensureDirectoryExists(nativeImageDir);

        File reflectConfigFile = new File(nativeImageDir, REFLECT_CONFIG_FILE);
//        if (!reflectConfigFile.exists()) {
//            throw new IOException("reflect-config.json not found in " + nativeImageDir.getAbsolutePath());
//        }
        return reflectConfigFile;
    }
 
    public static File getResourceConfigFile(String baseDir) throws IOException {
        File nativeImageDir = new File(baseDir, NATIVE_IMAGE_DIR);
        ensureDirectoryExists(nativeImageDir);

        File resourceConfigFile = new File(nativeImageDir, RESOURCE_CONFIG_FILE);
//        if (!resourceConfigFile.exists()) {
//            throw new IOException("resource-config.json not found in " + nativeImageDir.getAbsolutePath());
//        }
        return resourceConfigFile;
    }

    private static void ensureDirectoryExists(File dir) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
            }
        }
    }
}