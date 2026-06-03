package com.garganttua.core.configuration.provider;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.stream.Stream;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.source.ClasspathConfigurationSource;
import com.garganttua.core.observability.Logger;

/**
 * {@link IConfigProvider} that discovers configuration files on the classpath under
 * a base resource path (e.g. {@code garganttua/config}). Handles both exploded
 * directories ({@code file:} URLs) and packaged JARs ({@code jar:} URLs).
 *
 * @since 2.0.0-ALPHA02
 */
public class ClasspathConfigProvider implements IConfigProvider {

    private static final Logger log = Logger.getLogger(ClasspathConfigProvider.class);

    private final String basePath;
    private final int priority;

    /**
     * Creates a provider scanning the {@code garganttua/config} classpath path at priority 1.
     */
    public ClasspathConfigProvider() {
        this("garganttua/config", 1);
    }

    /**
     * @param basePath the classpath base path to scan (no leading slash, e.g. {@code garganttua/config})
     * @param priority ordering hint among providers (lower runs first)
     */
    public ClasspathConfigProvider(String basePath, int priority) {
        this.basePath = normalize(Objects.requireNonNull(basePath, "basePath"));
        this.priority = priority;
    }

    private static String normalize(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        return p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    @Override
    public String getName() {
        return "classpath:" + basePath;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public List<IConfigurationSource> discover() throws ConfigurationException {
        log.trace("Discovering classpath configuration under {}", basePath);
        List<IConfigurationSource> sources = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> roots = cl.getResources(basePath);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                switch (root.getProtocol()) {
                    case "file" -> discoverExploded(root, sources);
                    case "jar" -> discoverJar(root, sources);
                    default -> log.debug("Unsupported classpath URL protocol {} for {}", root.getProtocol(), root);
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException("Failed to scan classpath configuration under " + basePath, e);
        }
        log.debug("Discovered {} classpath configuration file(s) under {}", sources.size(), basePath);
        return sources;
    }

    private void discoverExploded(URL root, List<IConfigurationSource> sources) throws ConfigurationException {
        try {
            Path dir = Paths.get(root.toURI());
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> IConfigProvider.hasConfigExtension(p.getFileName().toString()))
                        .forEach(p -> {
                            String resource = basePath + "/" + dir.relativize(p).toString().replace('\\', '/');
                            sources.add(new ClasspathConfigurationSource(resource));
                        });
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to scan exploded classpath directory " + root, e);
        }
    }

    private void discoverJar(URL root, List<IConfigurationSource> sources) throws IOException {
        JarURLConnection conn = (JarURLConnection) root.openConnection();
        String prefix = basePath + "/";
        Enumeration<JarEntry> entries = conn.getJarFile().entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(prefix) && IConfigProvider.hasConfigExtension(name)) {
                sources.add(new ClasspathConfigurationSource(name));
            }
        }
    }
}
