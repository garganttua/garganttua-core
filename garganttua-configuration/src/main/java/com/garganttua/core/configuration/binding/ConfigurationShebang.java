package com.garganttua.core.configuration.binding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.observability.Logger;

/**
 * Reads the target-builder <em>alias</em> ("shebang") that a configuration file
 * declares, so the file is self-describing about which {@code @ConfigurableBuilder}
 * it configures.
 *
 * <p>Three equivalent forms are recognised, one per format family:</p>
 * <ul>
 *   <li>text (YAML / Properties / TOML): a first non-blank line {@code #!injection}
 *       (an optional {@code garganttua:} prefix is tolerated: {@code #!garganttua:injection});</li>
 *   <li>XML: a processing instruction {@code <?garganttua module="injection"?>} or a
 *       {@code module="injection"} attribute on the root element;</li>
 *   <li>JSON: a reserved top-level key {@code "$module": "injection"}.</li>
 * </ul>
 *
 * <p>Extraction is tolerant and format-agnostic: it scans the head of the raw content
 * for any of the forms, so it works regardless of the declared format hint.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public final class ConfigurationShebang {

    private static final Logger log = Logger.getLogger(ConfigurationShebang.class);

    /** Reserved JSON key carrying the target alias. */
    public static final String MODULE_KEY = "$module";

    private static final int HEAD_LIMIT = 8192;
    private static final Pattern SHEBANG_LINE = Pattern.compile("^#!\\s*(?:garganttua:)?([\\w.-]+)\\s*$");
    private static final Pattern XML_PI = Pattern.compile("<\\?garganttua\\s+module\\s*=\\s*\"([\\w.-]+)\"");
    private static final Pattern XML_ATTR = Pattern.compile("<\\w[^>]*\\bmodule\\s*=\\s*\"([\\w.-]+)\"");
    private static final Pattern JSON_KEY =
            Pattern.compile("\"\\$module\"\\s*:\\s*\"([\\w.-]+)\"");

    private ConfigurationShebang() {
    }

    /**
     * Extracts the target alias declared by the given source, if any.
     *
     * @param source the configuration source to inspect
     * @return the declared alias, or empty if the file declares none
     * @throws ConfigurationException if the source cannot be read
     */
    public static Optional<String> extract(IConfigurationSource source) throws ConfigurationException {
        String head = readHead(source);
        Optional<String> alias = extractFromText(head);
        if (alias.isPresent()) {
            log.debug("Configuration source [{}] targets alias '{}'", source.getDescription(), alias.get());
        } else {
            log.trace("No target alias (shebang) found in [{}]", source.getDescription());
        }
        return alias;
    }

    /**
     * Extracts the target alias from already-loaded raw content (no I/O).
     *
     * @param content the raw configuration content
     * @return the declared alias, or empty
     */
    public static Optional<String> extractFromText(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        // 1) #! shebang on the first non-blank line (text formats)
        for (String line : content.split("\\R", 64)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher m = SHEBANG_LINE.matcher(trimmed);
            return m.matches() ? Optional.of(m.group(1)) : firstStructured(content);
        }
        return firstStructured(content);
    }

    private static Optional<String> firstStructured(String content) {
        return firstMatch(JSON_KEY, content)
                .or(() -> firstMatch(XML_PI, content))
                .or(() -> firstMatch(XML_ATTR, content));
    }

    private static Optional<String> firstMatch(Pattern pattern, String content) {
        Matcher m = pattern.matcher(content);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static String readHead(IConfigurationSource source) throws ConfigurationException {
        try (InputStream is = source.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] buffer = new char[HEAD_LIMIT];
            int read = reader.read(buffer);
            return read <= 0 ? "" : new String(buffer, 0, read);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read configuration source " + source.getDescription(), e);
        }
    }
}
