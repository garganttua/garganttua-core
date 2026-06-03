package com.garganttua.core.configuration.provider;

import java.util.List;
import java.util.Set;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

/**
 * Discovers configuration files (sources) from some location — the classpath, the
 * filesystem, and later a remote configuration server.
 *
 * <p>
 * A provider knows <em>where</em> to look; it does not parse the files. Discovered
 * {@link IConfigurationSource}s are then read for their target-builder shebang and
 * applied to the matching {@code @ConfigurableBuilder}. Several providers can be
 * registered; this abstraction lets new locations (e.g. a {@code RemoteConfigProvider}
 * that syncs from a config server) be plugged in without touching the consumers.
 * </p>
 *
 * @since 2.0.0-ALPHA02
 */
public interface IConfigProvider {

    /** Configuration file extensions recognised by the built-in providers. */
    Set<String> CONFIG_EXTENSIONS = Set.of("json", "yml", "yaml", "xml", "toml", "properties");

    /**
     * @return a unique, human-readable name for this provider (diagnostics / source naming)
     */
    String getName();

    /**
     * @return ordering hint among providers (lower runs first); ties resolve in
     *         registration order
     */
    int getPriority();

    /**
     * Discovers the configuration sources currently exposed by this provider.
     *
     * @return the discovered sources (possibly empty, never {@code null})
     * @throws ConfigurationException if discovery fails irrecoverably
     */
    List<IConfigurationSource> discover() throws ConfigurationException;

    /** @return whether {@code fileName} carries a recognised configuration extension */
    static boolean hasConfigExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && CONFIG_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT));
    }
}
