package com.garganttua.core.configuration.source;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

public class EnvironmentConfigurationSource implements IConfigurationSource {

    private final String prefix;

    public EnvironmentConfigurationSource() {
        this(null);
    }

    public EnvironmentConfigurationSource(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public InputStream getInputStream() throws ConfigurationException {
        var env = System.getenv();
        var json = toJson(env);
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<String> getFormatHint() {
        return Optional.of("json");
    }

    @Override
    public String getDescription() {
        return "env" + (this.prefix != null ? "(" + this.prefix + ")" : "");
    }

    private String toJson(Map<String, String> env) {
        var filtered = env.entrySet().stream()
                .filter(e -> this.prefix == null || e.getKey().startsWith(this.prefix))
                .collect(Collectors.toMap(
                        e -> this.prefix != null ? e.getKey().substring(this.prefix.length()) : e.getKey(),
                        Map.Entry::getValue));

        var sb = new StringBuilder("{");
        var first = true;
        for (var entry : filtered.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(normalizeKey(entry.getKey()))).append("\":\"")
                    .append(escapeJson(entry.getValue())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String normalizeKey(String key) {
        return key.toLowerCase().replace('_', '.');
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
