package com.garganttua.core.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.injection.context.dsl.IPropertyProviderBuilder;
import com.garganttua.core.injection.context.properties.PropertyProvider;
import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

/**
 * Property provider builder that loads properties from {@code .properties} files.
 *
 * <p>Supports loading from:</p>
 * <ul>
 *   <li><b>Classpath</b> — files inside JARs or on the classpath (e.g. {@code application.properties})</li>
 *   <li><b>Filesystem</b> — absolute or relative file paths</li>
 * </ul>
 *
 * <p>When auto-detection is enabled, the builder automatically discovers
 * {@code application.properties} on the classpath. Multiple files with the same
 * name from different JARs are merged (later entries override earlier ones).</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Auto-detect application.properties from classpath
 * injectionContextBuilder
 *     .propertyProvider("config", PropertiesFileProviderBuilder.create(injectionContextBuilder)
 *         .autoDetect(true));
 *
 * // Load a specific file from filesystem
 * injectionContextBuilder
 *     .propertyProvider("config", PropertiesFileProviderBuilder.create(injectionContextBuilder)
 *         .file("/etc/myapp/config.properties"));
 *
 * // Load from classpath + override with filesystem file
 * injectionContextBuilder
 *     .propertyProvider("config", PropertiesFileProviderBuilder.create(injectionContextBuilder)
 *         .classpathResource("defaults.properties")
 *         .file("/etc/myapp/override.properties"));
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
@Slf4j
public class PropertiesFileProviderBuilder
        extends AbstractAutomaticLinkedBuilder<IPropertyProviderBuilder, IInjectionContextBuilder, IPropertyProvider>
        implements IPropertyProviderBuilder {

    private static final String DEFAULT_CLASSPATH_RESOURCE = "application.properties";

    private final List<PropertySource> sources = new ArrayList<>();

    public PropertiesFileProviderBuilder(IInjectionContextBuilder link) {
        super(link);
    }

    /**
     * Creates a new builder linked to the given injection context builder.
     */
    public static PropertiesFileProviderBuilder create(IInjectionContextBuilder link) {
        return new PropertiesFileProviderBuilder(link);
    }

    /**
     * Adds a classpath resource to load properties from.
     *
     * <p>If the resource exists in multiple JARs on the classpath, all
     * instances are loaded and merged (later JARs override earlier ones).</p>
     *
     * @param resourcePath the classpath resource path (e.g. "application.properties")
     * @return this builder for method chaining
     */
    public PropertiesFileProviderBuilder classpathResource(String resourcePath) {
        Objects.requireNonNull(resourcePath, "Resource path cannot be null");
        this.sources.add(new PropertySource(PropertySourceType.CLASSPATH, resourcePath));
        log.atDebug().log("Added classpath resource: {}", resourcePath);
        return this;
    }

    /**
     * Adds a filesystem file to load properties from.
     *
     * @param filePath the absolute or relative file path
     * @return this builder for method chaining
     */
    public PropertiesFileProviderBuilder file(String filePath) {
        Objects.requireNonNull(filePath, "File path cannot be null");
        this.sources.add(new PropertySource(PropertySourceType.FILE, filePath));
        log.atDebug().log("Added file source: {}", filePath);
        return this;
    }

    /**
     * Adds a filesystem file to load properties from.
     *
     * @param file the file
     * @return this builder for method chaining
     */
    public PropertiesFileProviderBuilder file(File file) {
        Objects.requireNonNull(file, "File cannot be null");
        return file(file.getAbsolutePath());
    }

    @Override
    public <PropertyType> IPropertyProviderBuilder withProperty(IClass<PropertyType> propertyType,
            String key, PropertyType property) throws DslException {
        // Manual property addition — stored as an explicit file-less source
        this.sources.add(new PropertySource(PropertySourceType.INLINE, key + "=" + property));
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atDebug().log("Auto-detecting {} on classpath", DEFAULT_CLASSPATH_RESOURCE);
        this.sources.add(0, new PropertySource(PropertySourceType.CLASSPATH, DEFAULT_CLASSPATH_RESOURCE));
    }

    @Override
    protected IPropertyProvider doBuild() throws DslException {
        Map<String, Object> allProperties = new LinkedHashMap<>();

        for (PropertySource source : sources) {
            try {
                Map<String, String> loaded = loadSource(source);
                allProperties.putAll(loaded);
                log.atDebug().log("Loaded {} properties from {} [{}]",
                        loaded.size(), source.type, source.path);
            } catch (IOException e) {
                log.atWarn().log("Failed to load properties from {} [{}]: {}",
                        source.type, source.path, e.getMessage());
            }
        }

        log.atDebug().log("Built PropertiesFileProvider with {} total properties", allProperties.size());
        return new PropertyProvider(allProperties);
    }

    private Map<String, String> loadSource(PropertySource source) throws IOException {
        return switch (source.type) {
            case CLASSPATH -> loadFromClasspath(source.path);
            case FILE -> loadFromFile(source.path);
            case INLINE -> loadInline(source.path);
        };
    }

    /**
     * Loads properties from all classpath resources matching the given path.
     * Multiple JARs contributing the same resource are merged.
     */
    private Map<String, String> loadFromClasspath(String resourcePath) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> resources = classLoader.getResources(resourcePath);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            log.atTrace().log("Loading properties from classpath URL: {}", url);
            try (InputStream is = url.openStream()) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                for (String key : props.stringPropertyNames()) {
                    result.put(key, props.getProperty(key));
                }
            }
        }

        if (result.isEmpty()) {
            // Try as single resource
            try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(is);
                    for (String key : props.stringPropertyNames()) {
                        result.put(key, props.getProperty(key));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Loads properties from a filesystem file.
     */
    private Map<String, String> loadFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            log.atWarn().log("Properties file not found: {}", filePath);
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        try (InputStream is = new FileInputStream(file)) {
            java.util.Properties props = new java.util.Properties();
            props.load(is);
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }
        }
        return result;
    }

    /**
     * Parses an inline "key=value" entry.
     */
    private Map<String, String> loadInline(String entry) {
        int eq = entry.indexOf('=');
        if (eq > 0) {
            return Map.of(entry.substring(0, eq), entry.substring(eq + 1));
        }
        return Map.of();
    }

    private enum PropertySourceType {
        CLASSPATH, FILE, INLINE
    }

    private record PropertySource(PropertySourceType type, String path) {}
}
