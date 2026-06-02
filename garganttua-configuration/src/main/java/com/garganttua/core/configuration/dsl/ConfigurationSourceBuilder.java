package com.garganttua.core.configuration.dsl;

import java.io.InputStream;
import java.nio.file.Path;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.source.ClasspathConfigurationSource;
import com.garganttua.core.configuration.source.FileConfigurationSource;
import com.garganttua.core.configuration.source.InputStreamConfigurationSource;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Linked builder that configures a single {@link IConfigurationSource} (file, classpath, stream,
 * or inline content) and its optional {@link IConfigurationFormat}. Sources are collected by the
 * parent {@link IConfigurationBuilder} rather than built independently.
 */
@Reflected
public class ConfigurationSourceBuilder extends AbstractLinkedBuilder<IConfigurationBuilder, Void>
        implements IConfigurationSourceBuilder {
    private static final Logger log = Logger.getLogger(ConfigurationSourceBuilder.class);

    private IConfigurationSource source;
    private IConfigurationFormat format;

    /**
     * @param link the parent configuration builder this source builder is linked to
     */
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

    /**
     * No-op build step; the configured source is collected by the parent builder.
     *
     * @return always {@code null}
     */
    @Override
    public Void build() throws DslException {
        // Sources are collected by the parent ConfigurationBuilder, not built independently
        return null;
    }
}
