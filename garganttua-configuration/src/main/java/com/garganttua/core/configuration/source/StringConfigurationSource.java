package com.garganttua.core.configuration.source;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

public class StringConfigurationSource implements IConfigurationSource {

    private final String content;
    private final String formatHint;

    public StringConfigurationSource(String content, String formatHint) {
        this.content = content;
        this.formatHint = formatHint;
    }

    @Override
    public InputStream getInputStream() throws ConfigurationException {
        return new ByteArrayInputStream(this.content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<String> getFormatHint() {
        return Optional.ofNullable(this.formatHint);
    }

    @Override
    public String getDescription() {
        return "inline(" + this.formatHint + ")";
    }
}
