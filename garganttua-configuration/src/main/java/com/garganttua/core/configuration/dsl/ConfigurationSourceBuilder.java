package com.garganttua.core.configuration.dsl;

import java.io.InputStream;
import java.nio.file.Path;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.source.ClasspathConfigurationSource;
import com.garganttua.core.configuration.source.FileConfigurationSource;
import com.garganttua.core.configuration.source.InputStreamConfigurationSource;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

import lombok.Getter;

@Reflected
public class ConfigurationSourceBuilder extends AbstractLinkedBuilder<IConfigurationBuilder, Void>
        implements IConfigurationSourceBuilder {
    private static final IDiagnostic log = Diagnostics.of(ConfigurationSourceBuilder.class);

    @Getter
    private IConfigurationSource source;
    @Getter
    private IConfigurationFormat format;

    public ConfigurationSourceBuilder(IConfigurationBuilder link) {
        super(link);
    }

    @Override
    public IConfigurationSourceBuilder file(Path path) {
        log.debug("Setting file source: {}", path);
        this.source = new FileConfigurationSource(path);
        return this;
    }

    @Override
    public IConfigurationSourceBuilder file(String path) {
        return file(Path.of(path));
    }

    @Override
    public IConfigurationSourceBuilder classpath(String resource) {
        log.debug("Setting classpath source: {}", resource);
        this.source = new ClasspathConfigurationSource(resource);
        return this;
    }

    @Override
    public IConfigurationSourceBuilder stream(InputStream stream) {
        log.debug("Setting stream source");
        this.source = new InputStreamConfigurationSource(stream);
        return this;
    }

    @Override
    public IConfigurationSourceBuilder inline(String content) {
        log.debug("Setting inline source");
        this.source = new StringConfigurationSource(content, this.format != null ? this.format.name() : "json");
        return this;
    }

    @Override
    public IConfigurationSourceBuilder format(IConfigurationFormat format) {
        log.debug("Setting format: {}", format.name());
        this.format = format;
        return this;
    }

    @Override
    public Void build() throws DslException {
        // Sources are collected by the parent ConfigurationBuilder, not built independently
        return null;
    }
}
