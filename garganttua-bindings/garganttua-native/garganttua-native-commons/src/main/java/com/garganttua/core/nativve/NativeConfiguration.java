package com.garganttua.core.nativve;

import com.garganttua.core.observability.Logger;
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

/**
 * Default {@link INativeConfiguration} that serializes collected reflection
 * entries and resource patterns into the GraalVM {@code reflect-config.json} and
 * {@code resource-config.json} files under {@code META-INF/native-image}.
 */
public class NativeConfiguration implements INativeConfiguration {
    private static final Logger log = Logger.getLogger(NativeConfiguration.class);

    private static final String NATIVE_IMAGE_DIR = "META-INF/native-image";
    private static final String REFLECT_CONFIG_FILE = "reflect-config.json";
    private static final String RESOURCE_CONFIG_FILE = "resource-config.json";

    private NativeConfigurationMode mode;
    private Set<IReflectionConfigurationEntry> collect;
    private Set<String> resources;
    private String resourcesPath;
    private String reflectionPath;
    private final String configNamespace;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a configuration writing to the flat (legacy) {@code META-INF/native-image} layout.
     *
     * @param mode           write mode (override or append)
     * @param collect        reflection entries to serialize
     * @param resources      resource patterns to include
     * @param resourcesPath  base directory for the resource config
     * @param reflectionPath base directory for the reflection config
     */
    public NativeConfiguration(NativeConfigurationMode mode, Set<IReflectionConfigurationEntry> collect,
            Set<String> resources, String resourcesPath, String reflectionPath) {
        this(mode, collect, resources, resourcesPath, reflectionPath, "");
    }

    /**
     * Creates a configuration writing under a namespaced {@code META-INF/native-image} layout.
     *
     * @param mode            write mode (override or append)
     * @param collect         reflection entries to serialize
     * @param resources       resource patterns to include
     * @param resourcesPath   base directory for the resource config
     * @param reflectionPath  base directory for the reflection config
     * @param configNamespace sub-path appended under {@code META-INF/native-image/}
     *        (e.g. {@code <groupId>/<artifactId>}) so each artifact writes its
     *        config to a unique location — the GraalVM-recommended layout that
     *        prevents collisions when several configs end up on one build path
     *        (uber-jar, tracing agent, downstream app). Empty = flat (legacy).
     */
    public NativeConfiguration(NativeConfigurationMode mode, Set<IReflectionConfigurationEntry> collect,
            Set<String> resources, String resourcesPath, String reflectionPath, String configNamespace) {
        log.trace("Creating NativeConfiguration with mode: {}", mode);
        this.mode = Objects.requireNonNull(mode, "Mode cannot be null");
        this.collect = Objects.requireNonNull(collect, "Reflection entries cannot be null");
        this.resources = Objects.requireNonNull(resources, "Resources cannot be null");
        this.resourcesPath = Objects.requireNonNull(resourcesPath, "Resources path cannot be null");
        this.reflectionPath = Objects.requireNonNull(reflectionPath, "Reflection path cannot be null");
        this.configNamespace = configNamespace != null ? configNamespace.trim() : "";
        log.debug("NativeConfiguration created with {} reflection entries, {} resources",
                collect.size(), resources.size());
    }

    /** {@code META-INF/native-image[/<configNamespace>]}. */
    private String nativeImageDir() {
        return this.configNamespace.isEmpty()
                ? NATIVE_IMAGE_DIR
                : NATIVE_IMAGE_DIR + File.separator + this.configNamespace;
    }

    @Override
    public void writeReflectionConfiguration() {
        log.trace("Entering writeReflectionConfiguration");
        try {
            String reflectConfigPath = this.reflectionPath+File.separator+nativeImageDir()+File.separator+REFLECT_CONFIG_FILE;
            log.debug("Writing reflection configuration to: {}", reflectConfigPath);
            this.ensureDirectoryExists(this.reflectionPath+File.separator+nativeImageDir());
            this.ensureFileExists(reflectConfigPath);
            OutputStream outputStream = new FileOutputStream(reflectConfigPath);
            InputStream inputStream = new FileInputStream(reflectConfigPath);
            this.writeReflectionConfiguration(inputStream, outputStream);
            log.debug("Reflection configuration written successfully to: {}", reflectConfigPath);
        } catch (IOException e) {
            log.error("Failed to write reflection configuration: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.trace("Exiting writeReflectionConfiguration");
    }

    @Override
    public void writeResourcesConfiguration() {
        log.trace("Entering writeResourcesConfiguration");
        try {
            String resourceConfigPath = this.resourcesPath+File.separator+nativeImageDir()+File.separator+RESOURCE_CONFIG_FILE;
            log.debug("Writing resources configuration to: {}", resourceConfigPath);
            this.ensureDirectoryExists(this.resourcesPath+File.separator+nativeImageDir());
            this.ensureFileExists(resourceConfigPath);
            OutputStream outputStream = new FileOutputStream(resourceConfigPath);
            InputStream inputStream = new FileInputStream(resourceConfigPath);
            this.writeResourcesConfiguration(inputStream, outputStream);
            log.debug("Resources configuration written successfully to: {}", resourceConfigPath);
        } catch (IOException e) {
            log.error("Failed to write resources configuration: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.trace("Exiting writeResourcesConfiguration");
    }

    @Override
    public void writeReflectionConfiguration(InputStream inputStream, OutputStream outputStream) {
        log.trace("Entering writeReflectionConfiguration with streams");
        try {
            log.debug("Serializing {} reflection entries to JSON", this.collect.size());
            outputStream.write(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.collect));
            log.debug("Reflection configuration written to output stream");
        } catch (IOException e) {
            log.error("Failed to write reflection configuration to stream: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.trace("Exiting writeReflectionConfiguration with streams");
    }

    @Override
    public void writeResourcesConfiguration(InputStream inputStream, OutputStream outputStream) {
        log.trace("Entering writeResourcesConfiguration with streams");
        try {
            log.debug("Building resource configuration with {} patterns", resources.size());
            Map<String, Object> resourceConfig = new HashMap<>();
            Map<String, List<Map<String, String>>> resourcesConfig = (Map<String, List<Map<String, String>>>) resourceConfig
                    .computeIfAbsent("resources", k -> new HashMap<>());
            List<Map<String, String>> includes = resourcesConfig.computeIfAbsent("includes", k -> new ArrayList<>());
            for (String pattern : resources) {
                includes.add(Map.of("pattern", pattern));

            }
            log.debug("Serializing resource configuration to JSON");
            outputStream.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(resourceConfig));
            log.debug("Resources configuration written to output stream");
        } catch (IOException e) {
            log.error("Failed to write resources configuration to stream: {}", e.getMessage());
            throw new NativeException(e);
        }
        log.trace("Exiting writeResourcesConfiguration with streams");
    }

    private void ensureDirectoryExists(String dir) throws IOException {
        log.trace("Ensuring directory exists: {}", dir);
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            log.debug("Creating directory: {}", dir);
            if (!dirFile.mkdirs()) {
                log.error("Failed to create directory: {}", dirFile.getAbsolutePath());
                throw new IOException("Failed to create directory: " + dirFile.getAbsolutePath());
            }
            log.debug("Created directory: {}", dir);
        }
    }

    private void ensureFileExists(String file) throws IOException {
        log.trace("Ensuring file exists: {}", file);
        File fFile = new File(file);
        if (!fFile.exists()) {
            log.debug("Creating file: {}", file);
            if (!fFile.createNewFile()) {
                log.error("Failed to create file: {}", fFile.getAbsolutePath());
                throw new IOException("Failed to create file: " + fFile.getAbsolutePath());
            }
            log.debug("Created file: {}", file);
        }
    }
}
