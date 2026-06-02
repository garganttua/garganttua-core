package com.garganttua.core.configuration.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationPopulator;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.format.PropertiesConfigurationFormat;
import com.garganttua.core.configuration.format.TomlConfigurationFormat;
import com.garganttua.core.configuration.format.XmlConfigurationFormat;
import com.garganttua.core.configuration.format.YamlConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Default {@link IConfigurationBuilder} implementation that collects configuration sources
 * and formats, then produces an {@link IConfigurationPopulator}. Resolves an {@link IReflection}
 * dependency and registers the available built-in formats when none are supplied explicitly.
 */
@Reflected
public class ConfigurationBuilder extends AbstractAutomaticDependentBuilder<IConfigurationBuilder, IConfigurationPopulator>
        implements IConfigurationBuilder {
    private static final Logger log = Logger.getLogger(ConfigurationBuilder.class);

    private final List<IConfigurationFormat> formats = new ArrayList<>();
    private final List<ConfigurationSourceBuilder> sourceBuilders = new ArrayList<>();
    private MethodMappingStrategy strategy = MethodMappingStrategy.SMART;
    private boolean strict = false;
    private IReflection reflection;

    /**
     * Creates a builder declaring its required {@link IReflectionBuilder} dependency.
     */
    public ConfigurationBuilder() {
        super(Set.of(
                DependencySpec.requireAutoDetect(IClass.getClass(IReflectionBuilder.class)),
                DependencySpec.require(IClass.getClass(IReflectionBuilder.class))));
        log.trace("Entering ConfigurationBuilder constructor");
        log.trace("Exiting ConfigurationBuilder constructor");
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        // No auto-detection behavior needed from IReflection dependency yet
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IReflection r) {
            log.debug("Received IReflection dependency");
            this.reflection = r;
        }
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build behavior needed
    }

    /**
     * @return a new configuration builder instance
     */
    public static IConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }

    @Override
    public IConfigurationSourceBuilder source() {
        var sourceBuilder = new ConfigurationSourceBuilder(this);
        this.sourceBuilders.add(sourceBuilder);
        return sourceBuilder;
    }

    @Override
    public IConfigurationBuilder withFormat(IConfigurationFormat format) {
        log.debug("Adding format: {}", format.name());
        this.formats.add(format);
        return this;
    }

    @Override
    public IConfigurationBuilder withMappingStrategy(String strategy) {
        log.debug("Setting mapping strategy: {}", strategy);
        this.strategy = MethodMappingStrategy.fromString(strategy);
        return this;
    }

    @Override
    public IConfigurationBuilder strict(boolean strict) throws DslException {
        this.strict = strict;
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.debug("Auto-detecting configuration formats");
        registerDefaultFormats();
    }

    @Override
    protected IConfigurationPopulator doBuild() throws DslException {
        log.debug("Building ConfigurationPopulator with {} formats", this.formats.size());

        if (this.formats.isEmpty()) {
            registerDefaultFormats();
        }

        return new BuilderPopulator(List.copyOf(this.formats), this.strategy, this.strict);
    }

    private void registerDefaultFormats() {
        registerFormatIfAbsent(new JsonConfigurationFormat());
        registerFormatIfAbsent(new YamlConfigurationFormat());
        registerFormatIfAbsent(new XmlConfigurationFormat());
        registerFormatIfAbsent(new PropertiesConfigurationFormat());
        registerFormatIfAbsent(new TomlConfigurationFormat());
    }

    private void registerFormatIfAbsent(IConfigurationFormat format) {
        if (!format.isAvailable()) {
            log.debug("Format {} not available, skipping", format.name());
            return;
        }
        var exists = this.formats.stream().anyMatch(f -> f.name().equals(format.name()));
        if (!exists) {
            log.debug("Registering format: {}", format.name());
            this.formats.add(format);
        }
    }

    /**
     * @return an immutable snapshot of the source builders registered via {@link #source()}
     */
    public List<ConfigurationSourceBuilder> getSourceBuilders() {
        return List.copyOf(this.sourceBuilders);
    }
}
