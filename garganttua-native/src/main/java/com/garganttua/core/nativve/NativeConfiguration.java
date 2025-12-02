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
        this.mode = Objects.requireNonNull(mode, "Mode cannot be null");
        this.collect = Objects.requireNonNull(collect, "Reflection entries cannot be null");
        this.resources = Objects.requireNonNull(resources, "Resources cannot be null");
        this.resourcesPath = Objects.requireNonNull(resourcesPath, "Resources path cannot be null");
        this.reflectionPath = Objects.requireNonNull(reflectionPath, "Reflection path cannot be null");
    }

    @Override
    public void writeReflectionConfiguration() {
        try {
            this.ensureDirectoryExists(this.reflectionPath+File.separator+NATIVE_IMAGE_DIR);
            this.ensureFileExists(this.reflectionPath+File.separator+NATIVE_IMAGE_DIR+File.separator+REFLECT_CONFIG_FILE);
            OutputStream outputStream = new FileOutputStream(this.reflectionPath+File.separator+NATIVE_IMAGE_DIR+File.separator+REFLECT_CONFIG_FILE);
            InputStream inputStream = new FileInputStream(this.reflectionPath+File.separator+NATIVE_IMAGE_DIR+File.separator+REFLECT_CONFIG_FILE);
            this.writeReflectionConfiguration(inputStream, outputStream);
        } catch (IOException e) {
            throw new NativeException(e);
        }
    }


    @Override
    public void writeResourcesConfiguration() {
        try {
            this.ensureDirectoryExists(this.resourcesPath+File.separator+NATIVE_IMAGE_DIR);
            this.ensureFileExists(this.resourcesPath+File.separator+NATIVE_IMAGE_DIR+File.separator+RESOURCE_CONFIG_FILE);
            OutputStream outputStream = new FileOutputStream(this.resourcesPath+File.separator+NATIVE_IMAGE_DIR+File.separator+RESOURCE_CONFIG_FILE);
            InputStream inputStream = new FileInputStream(this.resourcesPath+File.separator+NATIVE_IMAGE_DIR+File.separator+RESOURCE_CONFIG_FILE);
            this.writeResourcesConfiguration(inputStream, outputStream);
        } catch (IOException e) {
            throw new NativeException(e);
        }
    }

    @Override
    public void writeReflectionConfiguration(InputStream inputStream, OutputStream outputStream) {
        try {

            outputStream.write(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.collect));

        } catch (IOException e) {
            throw new NativeException(e);
        }
    }

    @Override
    public void writeResourcesConfiguration(InputStream inputStream, OutputStream outputStream) {
        try {
            Map<String, Object> resourceConfig = new HashMap<>();
            Map<String, List<Map<String, String>>> resourcesConfig = (Map<String, List<Map<String, String>>>) resourceConfig
                    .computeIfAbsent("resources", k -> new HashMap<>());
            List<Map<String, String>> includes = resourcesConfig.computeIfAbsent("includes", k -> new ArrayList<>());
            for (String pattern : resources) {
                includes.add(Map.of("pattern", pattern));

            }
            outputStream.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(resourceConfig));
        } catch (IOException e) {
            throw new NativeException(e);
        }
    }

    private void ensureDirectoryExists(String dir) throws IOException {
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            if (!dirFile.mkdirs()) {
                throw new IOException("Failed to create directory: " + dirFile.getAbsolutePath());
            }
        }
    }


    private void ensureFileExists(String file) throws IOException {
        File fFile = new File(file);
        if (!fFile.exists()) {
            if (!fFile.createNewFile()) {
                throw new IOException("Failed to create file: " + fFile.getAbsolutePath());
            }
        }
    }
}
