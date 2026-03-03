package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.format.JsonConfigurationFormat;

class JsonConfigurationFormatTest {

    @Test
    void testName() {
        var format = new JsonConfigurationFormat();
        assertEquals("json", format.name());
    }

    @Test
    void testExtensions() {
        var format = new JsonConfigurationFormat();
        assertTrue(format.extensions().contains("json"));
    }

    @Test
    void testMediaTypes() {
        var format = new JsonConfigurationFormat();
        assertTrue(format.mediaTypes().contains("application/json"));
    }

    @Test
    void testIsAvailable() {
        var format = new JsonConfigurationFormat();
        assertTrue(format.isAvailable());
    }

    @Test
    void testSupports() {
        var format = new JsonConfigurationFormat();
        assertTrue(format.supports("json"));
        assertTrue(format.supports("application/json"));
        assertFalse(format.supports("yaml"));
    }

    @Test
    void testParseSimpleObject() throws ConfigurationException {
        var format = new JsonConfigurationFormat();
        var json = """
                {"name": "test", "value": 42}
                """;
        var is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        var node = format.parse(is);

        assertTrue(node.isObject());
        assertEquals("test", node.get("name").get().asText().orElse(null));
        assertEquals(42, node.get("value").get().as(Integer.class).orElse(null));
    }

    @Test
    void testParseNestedObject() throws ConfigurationException {
        var format = new JsonConfigurationFormat();
        var json = """
                {
                    "server": {
                        "host": "localhost",
                        "port": 8080
                    },
                    "features": ["caching", "logging"]
                }
                """;
        var is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        var node = format.parse(is);

        assertTrue(node.isObject());
        var server = node.get("server").get();
        assertTrue(server.isObject());
        assertEquals("localhost", server.get("host").get().asText().orElse(null));

        var features = node.get("features").get();
        assertTrue(features.isArray());
        assertEquals(2, features.elements().size());
    }

    @Test
    void testParseInvalidJson() {
        var format = new JsonConfigurationFormat();
        var invalid = "{ invalid json }";
        var is = new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8));

        assertThrows(ConfigurationException.class, () -> format.parse(is));
    }

    @Test
    void testParseFromClasspath() throws ConfigurationException {
        var format = new JsonConfigurationFormat();
        var is = getClass().getClassLoader().getResourceAsStream("test-config.json");
        assertNotNull(is);

        var node = format.parse(is);
        assertTrue(node.isObject());
        assertEquals("myApp", node.get("name").get().asText().orElse(null));
        assertEquals(8080, node.get("port").get().as(Integer.class).orElse(null));
    }
}
