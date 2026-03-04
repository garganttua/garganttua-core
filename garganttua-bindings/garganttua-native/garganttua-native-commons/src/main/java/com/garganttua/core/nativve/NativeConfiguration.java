package com.garganttua.core.nativve;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NativeConfiguration implements INativeConfiguration {

    private static final String NATIVE_IMAGE_DIR = "META-INF/native-image";
    private static final String REFLECT_CONFIG_FILE = "reflect-config.json";
    private static final String RESOURCE_CONFIG_FILE = "resource-config.json";

    private NativeConfigurationMode mode;
    private Set<IReflectionConfigurationEntry> collect;
    private Set<String> resources;
    private String resourcesPath;
    private String reflectionPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NativeConfiguration(NativeConfigurationMode mode, Set<IReflectionConfigurationEntry> collect,
            Set<String> resources, String resourcesPath, String reflectionPath) {
        log.atTrace().log("Creating NativeConfiguration with mode: {}", mode);
        this.mode = Objects.requireNonNull(mode, "Mode cannot be null");
        this.collect = Objects.requireNonNull(collect, "Reflection entries cannot be null");
        this.resources = Objects.requireNonNull(resources, "Resources cannot be null");
        this.resourcesPath = Objects.requireNonNull(resourcesPath, "Resources path cannot be null");
        this.reflectionPath = Objects.requireNonNull(reflectionPath, "Reflection path cannot be null");
        log.atDebug().log("NativeConfiguration created with {} reflection entries, {} resources",
                collect.size(), resources.size());
    }

    @Override
    public void writeReflectionConfiguration() {
        log.atTrace().log("Entering writeReflectionConfiguration");
        try {
            String reflectConfigPath = this.reflectionPath+File.separator+NATIVE_IMAGE_DIR+File.separator+REFLECT_CONFIG_FILE;
            log.atDebug().log("Writing reflection configuration to: {}", reflectConfigPath);
            this.ensureDirectoryExists(this.reflectionPath+File.separator+NATIVE_IMAGE_DIR);
            this.ensureFileExists(reflectConfigPath);
            OutputStream outputStream = new FileOutputStream(reflectConfigPath);
            InputStream inputStream = new FileInputStream(reflectConfigPath);
            this.writeReflectionConfiguration(inputStream, outputStream);
            log.atDebug().log("Reflection configuration written successfully to: {}", reflectConfigPath);
        } catch (IOException e) {
            log.atError().log("Failed to write reflection configuration: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.atTrace().log("Exiting writeReflectionConfiguration");
    }


    @Override
    public void writeResourcesConfiguration() {
        log.atTrace().log("Entering writeResourcesConfiguration");
        try {
            String resourceConfigPath = this.resourcesPath+File.separator+NATIVE_IMAGE_DIR+File.separator+RESOURCE_CONFIG_FILE;
            log.atDebug().log("Writing resources configuration to: {}", resourceConfigPath);
            this.ensureDirectoryExists(this.resourcesPath+File.separator+NATIVE_IMAGE_DIR);
            this.ensureFileExists(resourceConfigPath);
            OutputStream outputStream = new FileOutputStream(resourceConfigPath);
            InputStream inputStream = new FileInputStream(resourceConfigPath);
            this.writeResourcesConfiguration(inputStream, outputStream);
            log.atDebug().log("Resources configuration written successfully to: {}", resourceConfigPath);
        } catch (IOException e) {
            log.atError().log("Failed to write resources configuration: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.atTrace().log("Exiting writeResourcesConfiguration");
    }

    @Override
    public void writeReflectionConfiguration(InputStream inputStream, OutputStream outputStream) {
        log.atTrace().log("Entering writeReflectionConfiguration with streams");
        try {
            log.atDebug().log("Serializing {} reflection entries to JSON", this.collect.size());
            outputStream.write(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.collect));
            log.atDebug().log("Reflection configuration written to output stream");
        } catch (IOException e) {
            log.atError().log("Failed to write reflection configuration to stream: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.atTrace().log("Exiting writeReflectionConfiguration with streams");
    }

    @Override
    public void writeResourcesConfiguration(InputStream inputStream, OutputStream outputStream) {
        log.atTrace().log("Entering writeResourcesConfiguration with streams");
        try {
            log.atDebug().log("Building resource configuration with {} patterns", resources.size());
            Map<String, Object> resourceConfig = new HashMap<>();
            Map<String, List<Map<String, String>>> resourcesConfig = (Map<String, List<Map<String, String>>>) resourceConfig
                    .computeIfAbsent("resources", k -> new HashMap<>());
            List<Map<String, String>> includes = resourcesConfig.computeIfAbsent("includes", k -> new ArrayList<>());
            for (String pattern : resources) {
                includes.add(Map.of("pattern", pattern));

            }
            log.atDebug().log("Serializing resource configuration to JSON");
            outputStream.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(resourceConfig));
            log.atDebug().log("Resources configuration written to output stream");
        } catch (IOException e) {
            log.atError().log("Failed to write resources configuration to stream: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.atTrace().log("Exiting writeResourcesConfiguration with streams");
    }

    private void ensureDirectoryExists(String dir) throws IOException {
        log.atTrace().log("Ensuring directory exists: {}", dir);
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            log.atDebug().log("Creating directory: {}", dir);
            if (!dirFile.mkdirs()) {
                log.atError().log("Failed to create directory: {}", dirFile.getAbsolutePath());
                throw new IOException("Failed to create directory: " + dirFile.getAbsolutePath());
            }
            log.atDebug().log("Created directory: {}", dir);
        }
    }


    private void ensureFileExists(String file) throws IOException {
        log.atTrace().log("Ensuring file exists: {}", file);
        File fFile = new File(file);
        if (!fFile.exists()) {
            log.atDebug().log("Creating file: {}", file);
            if (!fFile.createNewFile()) {
                log.atError().log("Failed to create file: {}", fFile.getAbsolutePath());
                throw new IOException("Failed to create file: " + fFile.getAbsolutePath());
            }
            log.atDebug().log("Created file: {}", file);
        }
    }
}
