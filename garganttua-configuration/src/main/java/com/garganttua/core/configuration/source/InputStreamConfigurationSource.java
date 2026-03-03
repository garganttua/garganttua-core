package com.garganttua.core.configuration.source;

import java.io.InputStream;
import java.util.Optional;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

public class InputStreamConfigurationSource implements IConfigurationSource {

    private final InputStream inputStream;
    private final String formatHint;

    public InputStreamConfigurationSource(InputStream inputStream) {
        this(inputStream, null);
    }

    public InputStreamConfigurationSource(InputStream inputStream, String formatHint) {
        this.inputStream = inputStream;
        this.formatHint = formatHint;
    }

    @Override
    public InputStream getInputStream() throws ConfigurationException {
        return this.inputStream;
    }

    @Override
    public Optional<String> getFormatHint() {
        return Optional.ofNullable(this.formatHint);
    }

    @Override
    public String getDescription() {
        return "stream" + (this.formatHint != null ? "(" + this.formatHint + ")" : "");
    }
}
