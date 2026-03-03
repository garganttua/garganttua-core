package com.garganttua.core.configuration.source;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

public class FileConfigurationSource implements IConfigurationSource {

    private final Path path;

    public FileConfigurationSource(Path path) {
        this.path = path;
    }

    public FileConfigurationSource(String path) {
        this.path = Path.of(path);
    }

    @Override
    public InputStream getInputStream() throws ConfigurationException {
        try {
            return new FileInputStream(this.path.toFile());
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Configuration file not found: " + this.path, e);
        }
    }

    @Override
    public Optional<String> getFormatHint() {
        var fileName = this.path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return Optional.of(fileName.substring(dot + 1));
        }
        return Optional.empty();
    }

    @Override
    public String getDescription() {
        return "file:" + this.path;
    }
}
