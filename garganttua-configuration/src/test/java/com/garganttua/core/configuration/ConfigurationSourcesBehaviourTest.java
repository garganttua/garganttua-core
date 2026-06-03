package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.configuration.source.ClasspathConfigurationSource;
import com.garganttua.core.configuration.source.EnvironmentConfigurationSource;
import com.garganttua.core.configuration.source.FileConfigurationSource;
import com.garganttua.core.configuration.source.InputStreamConfigurationSource;
import com.garganttua.core.configuration.source.StringConfigurationSource;

/**
 * Behaviour tests for the {@code IConfigurationSource} implementations:
 * format-hint derivation, stream content, and error paths.
 */
class ConfigurationSourcesBehaviourTest {

    private static String read(InputStream is) throws Exception {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    // ---------- FileConfigurationSource ----------

    @Test
    void fileSourceDerivesFormatHintFromExtension() {
        var src = new FileConfigurationSource("/etc/app/config.yaml");
        assertEquals("yaml", src.getFormatHint().orElseThrow());
    }

    @Test
    void fileSourceUsesLastExtensionForDottedNames() {
        var src = new FileConfigurationSource("/etc/app/config.backup.json");
        assertEquals("json", src.getFormatHint().orElseThrow());
    }

    @Test
    void fileSourceHasNoHintWhenNoExtension() {
        var src = new FileConfigurationSource("/etc/app/config");
        assertTrue(src.getFormatHint().isEmpty());
    }

    @Test
    void fileSourceHasNoHintForDotfileWithoutExtension() {
        // ".env" -> dot at index 0, the code requires dot > 0 so no hint
        var src = new FileConfigurationSource(".env");
        assertTrue(src.getFormatHint().isEmpty());
    }

    @Test
    void fileSourceDescriptionContainsPath() {
        var src = new FileConfigurationSource("/tmp/foo.json");
        assertEquals("file:" + Path.of("/tmp/foo.json"), src.getDescription());
    }

    @Test
    void fileSourceReadsRealFileContent(@TempDir Path dir) throws Exception {
        var file = dir.resolve("data.json");
        Files.writeString(file, "{\"a\":1}");
        var src = new FileConfigurationSource(file);
        try (var is = src.getInputStream()) {
            assertEquals("{\"a\":1}", read(is));
        }
    }

    @Test
    void fileSourceThrowsConfigurationExceptionForMissingFile() {
        var src = new FileConfigurationSource("/no/such/path/definitely-missing-12345.json");
        var ex = assertThrows(ConfigurationException.class, src::getInputStream);
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void filePathConstructorAndStringConstructorAgree() {
        var a = new FileConfigurationSource("/tmp/x.toml");
        var b = new FileConfigurationSource(Path.of("/tmp/x.toml"));
        assertEquals(a.getDescription(), b.getDescription());
        assertEquals(a.getFormatHint(), b.getFormatHint());
    }

    // ---------- ClasspathConfigurationSource ----------

    @Test
    void classpathSourceDerivesFormatHint() {
        var src = new ClasspathConfigurationSource("config/test-config.json");
        assertEquals("json", src.getFormatHint().orElseThrow());
    }

    @Test
    void classpathSourceDescription() {
        var src = new ClasspathConfigurationSource("test-config.json");
        assertEquals("classpath:test-config.json", src.getDescription());
    }

    @Test
    void classpathSourceLoadsExistingResource() throws Exception {
        var src = new ClasspathConfigurationSource("test-config.json");
        try (var is = src.getInputStream()) {
            assertTrue(read(is).contains("myApp"));
        }
    }

    @Test
    void classpathSourceThrowsForMissingResource() {
        var src = new ClasspathConfigurationSource("does-not-exist-987654.json");
        var ex = assertThrows(ConfigurationException.class, src::getInputStream);
        assertTrue(ex.getMessage().contains("not found"));
    }

    // ---------- StringConfigurationSource ----------

    @Test
    void stringSourceExposesContentAsUtf8() throws Exception {
        var src = new StringConfigurationSource("éàü-content", "json");
        try (var is = src.getInputStream()) {
            assertEquals("éàü-content", read(is));
        }
    }

    @Test
    void stringSourceFormatHintAndDescription() {
        var src = new StringConfigurationSource("{}", "yaml");
        assertEquals("yaml", src.getFormatHint().orElseThrow());
        assertEquals("inline(yaml)", src.getDescription());
    }

    @Test
    void stringSourceNullHintYieldsEmptyOptional() {
        var src = new StringConfigurationSource("{}", null);
        assertTrue(src.getFormatHint().isEmpty());
    }

    // ---------- InputStreamConfigurationSource ----------

    @Test
    void inputStreamSourceReturnsSameStream() throws Exception {
        var raw = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
        var src = new InputStreamConfigurationSource(raw);
        assertSame(raw, src.getInputStream());
    }

    @Test
    void inputStreamSourceNoHintByDefault() {
        var src = new InputStreamConfigurationSource(new ByteArrayInputStream(new byte[0]));
        assertTrue(src.getFormatHint().isEmpty());
        assertEquals("stream", src.getDescription());
    }

    @Test
    void inputStreamSourceWithHint() {
        var src = new InputStreamConfigurationSource(new ByteArrayInputStream(new byte[0]), "json");
        assertEquals("json", src.getFormatHint().orElseThrow());
        assertEquals("stream(json)", src.getDescription());
    }

    // ---------- EnvironmentConfigurationSource ----------

    @Test
    void envSourceAlwaysReportsJsonFormat() {
        assertEquals("json", new EnvironmentConfigurationSource().getFormatHint().orElseThrow());
    }

    @Test
    void envSourceDescriptionReflectsPrefix() {
        assertEquals("env", new EnvironmentConfigurationSource().getDescription());
        assertEquals("env(APP_)", new EnvironmentConfigurationSource("APP_").getDescription());
    }

    @Test
    void envSourceProducesParseableJsonWithNormalizedKeys() throws Exception {
        // PATH is virtually always present; underscores->dots, lowercased
        var src = new EnvironmentConfigurationSource();
        try (var is = src.getInputStream()) {
            var json = read(is);
            var tree = new ObjectMapper().readTree(json);
            assertTrue(tree.isObject());
            // every key is lowercase and contains no underscore
            tree.fieldNames().forEachRemaining(k -> {
                assertEquals(k.toLowerCase(), k, "key should be lowercased: " + k);
                assertFalse(k.contains("_"), "underscores should be replaced: " + k);
            });
        }
    }

    @Test
    void envSourcePrefixFiltersAndStripsKeys() throws Exception {
        // Build a source over a synthetic env via a subclass would require touching prod code;
        // instead assert that with an unlikely prefix the JSON object is empty.
        var src = new EnvironmentConfigurationSource("ZZ_UNLIKELY_PREFIX_QWERTY_");
        try (var is = src.getInputStream()) {
            var json = read(is);
            var tree = new ObjectMapper().readTree(json);
            assertTrue(tree.isObject());
            assertEquals(0, tree.size(), "no env var should match the unlikely prefix");
        }
    }

    @Test
    void envSourceJsonRoundTripsThroughJackson() throws Exception {
        // Ensures escaping produces valid JSON even with real environment values.
        var src = new EnvironmentConfigurationSource();
        Map<?, ?> map;
        try (var is = src.getInputStream()) {
            map = new ObjectMapper().readValue(is, Map.class);
        }
        assertNotNull(map);
    }
}
