package com.garganttua.core.configuration;

import java.io.InputStream;
import java.util.Optional;

public interface IConfigurationSource {

    InputStream getInputStream() throws ConfigurationException;

    Optional<String> getFormatHint();

    String getDescription();
}
