package com.garganttua.core.configuration;

import java.io.InputStream;
import java.util.Set;

public interface IConfigurationFormat {

    String name();

    Set<String> extensions();

    Set<String> mediaTypes();

    IConfigurationNode parse(InputStream input) throws ConfigurationException;

    boolean supports(String extensionOrMediaType);

    boolean isAvailable();
}
