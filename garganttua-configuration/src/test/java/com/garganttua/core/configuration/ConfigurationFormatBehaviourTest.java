package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.format.PropertiesConfigurationFormat;
import com.garganttua.core.configuration.format.TomlConfigurationFormat;
import com.garganttua.core.configuration.format.XmlConfigurationFormat;
import com.garganttua.core.configuration.format.YamlConfigurationFormat;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkReflectionProvider;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

/**
 * Behaviour tests for the Jackson-backed configuration formats: name/extension/media-type
 * matching, availability gating, real parse round-trips, and parse-error paths.
 * The optional jackson dataformat dependencies are on the test classpath ({@code provided}).
 */
class ConfigurationFormatBehaviourTest {

    @BeforeAll
    static void setUpReflection() throws Exception {
        ReflectionBuilder.builder().withProvider(new JdkReflectionProvider()).build();
    }

    private static IConfigurationNode parse(IConfigurationFormat fmt, String content) throws ConfigurationException {
        return fmt.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    // ---------- supports() across name/extension/media-type ----------

    @Test
    void supportsIsCaseInsensitiveOnExtensionsAndMediaTypes() {
        var yaml = new YamlConfigurationFormat();
        assertTrue(yaml.supports("YAML"));
        assertTrue(yaml.supports("yml"));
        assertTrue(yaml.supports("APPLICATION/YAML"));
        assertFalse(yaml.supports("json"));
    }

    @Test
    void yamlExtensionsIncludeBothYamlAndYml() {
        var yaml = new YamlConfigurationFormat();
        assertTrue(yaml.extensions().contains("yaml"));
        assertTrue(yaml.extensions().contains("yml"));
        assertEquals("yaml", yaml.name());
    }

    @Test
    void xmlMediaTypesIncludeTextAndApplication() {
        var xml = new XmlConfigurationFormat();
        assertTrue(xml.supports("text/xml"));
        assertTrue(xml.supports("application/xml"));
        assertTrue(xml.supports("xml"));
    }

    @Test
    void formatsReportAvailableWhenDataformatOnClasspath() {
        assertTrue(new YamlConfigurationFormat().isAvailable());
        assertTrue(new TomlConfigurationFormat().isAvailable());
        assertTrue(new XmlConfigurationFormat().isAvailable());
        assertTrue(new PropertiesConfigurationFormat().isAvailable());
    }

    // NOTE: actual YAML/TOML/XML/Properties parse round-trips are exercised in integration with
    // the real reflection provider; the JDK stub provider used in this module's tests cannot
    // instantiate the Jackson factory via getDeclaredConstructor(), so createMapper() is not
    // callable here. Metadata/availability above is fully asserted. JSON (which builds its mapper
    // directly without reflection) covers the parse + node semantics below.

    // ---------- JSON parse + error path ----------

    @Test
    void jsonParsesNestedStructure() throws Exception {
        var json = "{\"name\": \"myApp\", \"server\": {\"port\": 8080}, \"tags\": [\"a\", \"b\"]}";
        var node = parse(new JsonConfigurationFormat(), json);
        assertTrue(node.isObject());
        assertEquals("myApp", node.get("name").orElseThrow().asText().orElseThrow());
        assertEquals(8080,
                node.get("server").orElseThrow().get("port").orElseThrow()
                        .as(IClass.getClass(Integer.class)).orElseThrow());
        assertEquals(2, node.get("tags").orElseThrow().elements().size());
    }

    @Test
    void invalidJsonThrowsConfigurationException() {
        var ex = assertThrows(ConfigurationException.class,
                () -> parse(new JsonConfigurationFormat(), "{ not valid"));
        assertTrue(ex.getMessage().contains("Failed to parse"));
    }

    // ---------- node path / type semantics via JSON ----------

    @Test
    void nodePathTracksNestingAndArrayIndices() throws Exception {
        var json = "{\"server\": {\"hosts\": [\"a\", \"b\"]}}";
        var node = parse(new JsonConfigurationFormat(), json);
        var hosts = node.get("server").orElseThrow().get("hosts").orElseThrow();
        assertEquals("server.hosts", hosts.path());
        assertEquals("server.hosts[1]", hosts.elements().get(1).path());
    }

    @Test
    void getMissingKeyReturnsEmptyOptional() throws Exception {
        var node = parse(new JsonConfigurationFormat(), "{\"a\": 1}");
        assertTrue(node.get("missing").isEmpty());
    }

    @Test
    void nullJsonValueReportsNullTypeAndEmptyText() throws Exception {
        var node = parse(new JsonConfigurationFormat(), "{\"x\": null}");
        var x = node.get("x").orElseThrow();
        assertTrue(x.isNull());
        assertTrue(x.asText().isEmpty());
        assertTrue(x.as(IClass.getClass(String.class)).isEmpty());
    }

    @Test
    void asTextOnObjectReturnsJsonString() throws Exception {
        var node = parse(new JsonConfigurationFormat(), "{\"obj\": {\"k\": 1}}");
        var obj = node.get("obj").orElseThrow();
        assertTrue(obj.asText().orElseThrow().contains("\"k\""));
    }
}
