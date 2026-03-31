package com.garganttua.core.configuration.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigurationBuilder extends AbstractAutomaticDependentBuilder<IConfigurationBuilder, IConfigurationPopulator>
        implements IConfigurationBuilder {

    private final List<IConfigurationFormat> formats = new ArrayList<>();
    private final List<ConfigurationSourceBuilder> sourceBuilders = new ArrayList<>();
    private MethodMappingStrategy strategy = MethodMappingStrategy.SMART;
    private boolean strict = false;
    private IReflection reflection;

    public ConfigurationBuilder() {
        super(Set.of(DependencySpec.require(IClass.getClass(IReflectionBuilder.class), DependencyPhase.BOTH)));
        log.atTrace().log("Entering ConfigurationBuilder constructor");
        log.atTrace().log("Exiting ConfigurationBuilder constructor");
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        // No auto-detection behavior needed from IReflection dependency yet
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IReflection r) {
            log.atDebug().log("Received IReflection dependency");
            this.reflection = r;
        }
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build behavior needed
    }

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
        log.atDebug().log("Adding format: {}", format.name());
        this.formats.add(format);
        return this;
    }

    @Override
    public IConfigurationBuilder withMappingStrategy(String strategy) {
        log.atDebug().log("Setting mapping strategy: {}", strategy);
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
        log.atDebug().log("Auto-detecting configuration formats");
        registerDefaultFormats();
    }

    @Override
    protected IConfigurationPopulator doBuild() throws DslException {
        log.atDebug().log("Building ConfigurationPopulator with {} formats", this.formats.size());

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
            log.atDebug().log("Format {} not available, skipping", format.name());
            return;
        }
        var exists = this.formats.stream().anyMatch(f -> f.name().equals(format.name()));
        if (!exists) {
            log.atDebug().log("Registering format: {}", format.name());
            this.formats.add(format);
        }
    }

    public List<ConfigurationSourceBuilder> getSourceBuilders() {
        return List.copyOf(this.sourceBuilders);
    }
}
