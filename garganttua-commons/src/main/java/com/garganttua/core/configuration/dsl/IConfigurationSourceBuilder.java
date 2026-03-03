package com.garganttua.core.configuration.dsl;

import java.io.InputStream;
import java.nio.file.Path;

import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.dsl.ILinkedBuilder;

public interface IConfigurationSourceBuilder extends ILinkedBuilder<IConfigurationBuilder, Void> {

    IConfigurationSourceBuilder file(Path path);

    IConfigurationSourceBuilder file(String path);

    IConfigurationSourceBuilder classpath(String resource);

    IConfigurationSourceBuilder stream(InputStream stream);

    IConfigurationSourceBuilder inline(String content);

    IConfigurationSourceBuilder format(IConfigurationFormat format);
}
