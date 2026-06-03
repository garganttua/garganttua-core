package com.garganttua.core.configuration.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.source.FileConfigurationSource;
import com.garganttua.core.observability.Logger;

/**
 * {@link IConfigProvider} that discovers configuration files under a filesystem
 * directory (optionally recursively).
 *
 * @since 2.0.0-ALPHA02
 */
public class FileSystemConfigProvider implements IConfigProvider {

    private static final Logger log = Logger.getLogger(FileSystemConfigProvider.class);

    private final Path directory;
    private final boolean recursive;
    private final int priority;

    /**
     * Creates a provider scanning {@code directory} (recursively) at priority 0.
     *
     * @param directory the directory to scan
     */
    public FileSystemConfigProvider(Path directory) {
        this(directory, true, 0);
    }

    /**
     * @param directory the directory to scan for configuration files
     * @param recursive whether to descend into sub-directories
     * @param priority  ordering hint among providers (lower runs first)
     */
    public FileSystemConfigProvider(Path directory, boolean recursive, int priority) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.recursive = recursive;
        this.priority = priority;
    }

    @Override
    public String getName() {
        return "filesystem:" + directory;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public List<IConfigurationSource> discover() throws ConfigurationException {
        log.trace("Discovering configuration files under {}", directory);
        if (!Files.isDirectory(directory)) {
            log.debug("Configuration directory {} does not exist; nothing discovered", directory);
            return List.of();
        }
        List<IConfigurationSource> sources = new ArrayList<>();
        try (Stream<Path> walk = recursive ? Files.walk(directory) : Files.list(directory)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> IConfigProvider.hasConfigExtension(p.getFileName().toString()))
                    .forEach(p -> sources.add(new FileConfigurationSource(p)));
        } catch (IOException e) {
            throw new ConfigurationException("Failed to scan configuration directory " + directory, e);
        }
        log.debug("Discovered {} configuration file(s) under {}", sources.size(), directory);
        return sources;
    }
}
