package com.garganttua.core.configuration.dsl;

import java.io.InputStream;
import java.nio.file.Path;

import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.source.ClasspathConfigurationSource;
import com.garganttua.core.configuration.source.FileConfigurationSource;
import com.garganttua.core.configuration.source.InputStreamConfigurationSource;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigurationSourceBuilder extends AbstractLinkedBuilder<IConfigurationBuilder, Void>
        implements IConfigurationSourceBuilder {

    @Getter
    private IConfigurationSource source;
    @Getter
    private IConfigurationFormat format;

    public ConfigurationSourceBuilder(IConfigurationBuilder link) {
        super(link);
    }

    @Override
    public IConfigurationSourceBuilder file(Path path) {
        log.atDebug().log("Setting file source: {}", path);
        this.source = new FileConfigurationSource(path);
        return this;
    }

    @Override
    public IConfigurationSourceBuilder file(String path) {
        return file(Path.of(path));
    }

    @Override
    public IConfigurationSourceBuilder classpath(String resource) {
        log.atDebug().log("Setting classpath source: {}", resource);
        this.source = new ClasspathConfigurationSource(resource);
        return this;
    }

    @Override
    public IConfigurationSourceBuilder stream(InputStream stream) {
        log.atDebug().log("Setting stream source");
        this.source = new InputStreamConfigurationSource(stream);
        return this;
    }

    @Override
    public IConfigurationSourceBuilder inline(String content) {
        log.atDebug().log("Setting inline source");
        this.source = new StringConfigurationSource(content, this.format != null ? this.format.name() : "json");
        return this;
    }

    @Override
    public IConfigurationSourceBuilder format(IConfigurationFormat format) {
        log.atDebug().log("Setting format: {}", format.name());
        this.format = format;
        return this;
    }

    @Override
    public Void build() throws DslException {
        // Sources are collected by the parent ConfigurationBuilder, not built independently
        return null;
    }
}
