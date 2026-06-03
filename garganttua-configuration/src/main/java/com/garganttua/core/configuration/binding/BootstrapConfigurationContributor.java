package com.garganttua.core.configuration.binding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.core.bootstrap.dsl.IBootstrapConfigurationContributor;
import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.format.PropertiesConfigurationFormat;
import com.garganttua.core.configuration.format.TomlConfigurationFormat;
import com.garganttua.core.configuration.format.XmlConfigurationFormat;
import com.garganttua.core.configuration.format.YamlConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.provider.ClasspathConfigProvider;
import com.garganttua.core.configuration.provider.IConfigProvider;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.observability.Logger;

/**
 * Bootstrap CONFIGURATION-stage contributor that auto-wires external configuration
 * files to the builders they target.
 *
 * <p>Discovered by {@code Bootstrap} via {@link java.util.ServiceLoader}, it is active
 * only when {@code garganttua-configuration} is on the classpath (the wanted "optional
 * dependent" behaviour). For each discovered configuration file it reads the target
 * alias (shebang), and for every builder annotated {@code @ConfigurableBuilder} whose
 * alias matches, it applies the file to that builder before it builds.</p>
 *
 * <p><b>Precedence:</b> configuration is applied at the CONFIGURATION stage, after the
 * builder has been constructed (so any programmatic setup already ran) and before it
 * builds. For scalar setters this means the configuration file wins (it is applied last);
 * for collection-style builders the items merge into the builder's existing sources. A
 * dedicated, higher-priority {@code "configuration"} {@code MultiSourceCollector} source
 * is a planned refinement.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public class BootstrapConfigurationContributor implements IBootstrapConfigurationContributor {

    private static final Logger log = Logger.getLogger(BootstrapConfigurationContributor.class);

    private final List<IConfigProvider> providers;

    /** Uses the default classpath provider ({@code garganttua/config}). */
    public BootstrapConfigurationContributor() {
        this(List.of(new ClasspathConfigProvider()));
    }

    /**
     * @param providers the configuration providers to discover sources from
     */
    public BootstrapConfigurationContributor(List<IConfigProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public void contribute(List<IBuilder<?>> builders) {
        Map<String, IConfigurationSource> byAlias = discoverByAlias();
        if (byAlias.isEmpty()) {
            log.trace("No targeted configuration files discovered");
            return;
        }
        ConfigurationApplier applier = new ConfigurationApplier(defaultPopulator());
        for (IBuilder<?> builder : builders) {
            ConfigurableBuilder marker = builder.getClass().getAnnotation(ConfigurableBuilder.class);
            if (marker == null) {
                continue;
            }
            IConfigurationSource source = byAlias.get(marker.value());
            if (source == null) {
                continue;
            }
            try {
                applier.apply(builder, source);
                log.info("Applied configuration [{}] to @ConfigurableBuilder(\"{}\") {}",
                        source.getDescription(), marker.value(), builder.getClass().getSimpleName());
            } catch (ConfigurationException e) {
                log.error("Failed to apply configuration [{}] to alias '{}': {}",
                        source.getDescription(), marker.value(), e.getMessage(), e);
            }
        }
    }

    /** Discovers all sources across providers (priority order) and keys them by target alias. */
    private Map<String, IConfigurationSource> discoverByAlias() {
        Map<String, IConfigurationSource> byAlias = new LinkedHashMap<>();
        List<IConfigProvider> ordered = new ArrayList<>(providers);
        ordered.sort(Comparator.comparingInt(IConfigProvider::getPriority));
        for (IConfigProvider provider : ordered) {
            try {
                for (IConfigurationSource source : provider.discover()) {
                    ConfigurationShebang.extract(source).ifPresent(alias -> {
                        if (byAlias.putIfAbsent(alias, source) != null) {
                            log.warn("Multiple configuration files target alias '{}'; keeping the higher-priority one",
                                    alias);
                        }
                    });
                }
            } catch (ConfigurationException e) {
                log.error("Configuration provider {} failed to discover sources: {}",
                        provider.getName(), e.getMessage(), e);
            }
        }
        return byAlias;
    }

    private static BuilderPopulator defaultPopulator() {
        List<IConfigurationFormat> formats = new ArrayList<>();
        addIfAvailable(formats, new JsonConfigurationFormat());
        addIfAvailable(formats, new YamlConfigurationFormat());
        addIfAvailable(formats, new XmlConfigurationFormat());
        addIfAvailable(formats, new PropertiesConfigurationFormat());
        addIfAvailable(formats, new TomlConfigurationFormat());
        return new BuilderPopulator(formats, MethodMappingStrategy.SMART, false);
    }

    private static void addIfAvailable(List<IConfigurationFormat> formats, IConfigurationFormat format) {
        if (format.isAvailable()) {
            formats.add(format);
        }
    }
}
