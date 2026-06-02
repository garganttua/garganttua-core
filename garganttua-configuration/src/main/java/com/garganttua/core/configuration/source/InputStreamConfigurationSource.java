package com.garganttua.core.configuration.source;

import java.io.InputStream;
import java.util.Optional;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

/**
 * {@link IConfigurationSource} backed by an already-open {@link InputStream}, with an
 * optional explicit format hint.
 */
public class InputStreamConfigurationSource implements IConfigurationSource {

    private final InputStream inputStream;
    private final String formatHint;

    /**
     * Creates a source over the given stream with no format hint.
     *
     * @param inputStream the configuration input stream
     */
    public InputStreamConfigurationSource(InputStream inputStream) {
        this(inputStream, null);
    }

    /**
     * Creates a source over the given stream with an explicit format hint.
     *
     * @param inputStream the configuration input stream
     * @param formatHint  the format hint (e.g. {@code "json"}), may be {@code null}
     */
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
