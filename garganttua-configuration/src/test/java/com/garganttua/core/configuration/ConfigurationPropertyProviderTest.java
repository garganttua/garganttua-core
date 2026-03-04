package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.integration.ConfigurationPropertyProvider;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.reflection.IClass;

class ConfigurationPropertyProviderTest {

    @Test
    void testFlattenSimple() throws Exception {
        var node = parseJson("""
                {"name": "test", "port": "8080"}
                """);

        var provider = new ConfigurationPropertyProvider(node);
        assertEquals("test", provider.getProperty("name", IClass.getClass(String.class)).orElse(null));
        assertEquals("8080", provider.getProperty("port", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testFlattenNested() throws Exception {
        var node = parseJson("""
                {
                    "database": {
                        "host": "localhost",
                        "port": "5432"
                    }
                }
                """);

        var provider = new ConfigurationPropertyProvider(node);
        assertEquals("localhost", provider.getProperty("database.host", IClass.getClass(String.class)).orElse(null));
        assertEquals("5432", provider.getProperty("database.port", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testFlattenArray() throws Exception {
        var node = parseJson("""
                {"tags": ["web", "api"]}
                """);

        var provider = new ConfigurationPropertyProvider(node);
        assertEquals("web", provider.getProperty("tags[0]", IClass.getClass(String.class)).orElse(null));
        assertEquals("api", provider.getProperty("tags[1]", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testKeys() throws Exception {
        var node = parseJson("""
                {"a": "1", "b": "2", "c": "3"}
                """);

        var provider = new ConfigurationPropertyProvider(node);
        var keys = provider.keys();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains("c"));
    }

    @Test
    void testTypeConversion() throws Exception {
        var node = parseJson("""
                {"count": "42", "enabled": "true", "ratio": "3.14"}
                """);

        var provider = new ConfigurationPropertyProvider(node);
        assertEquals(42, provider.getProperty("count", IClass.getClass(Integer.class)).orElse(null));
        assertTrue(provider.getProperty("enabled", IClass.getClass(Boolean.class)).orElse(false));
        assertEquals(3.14, provider.getProperty("ratio", IClass.getClass(Double.class)).orElse(0.0), 0.001);
    }

    @Test
    void testMissingProperty() throws Exception {
        var node = parseJson("""
                {"name": "test"}
                """);

        var provider = new ConfigurationPropertyProvider(node);
        assertTrue(provider.getProperty("missing", IClass.getClass(String.class)).isEmpty());
    }

    @Test
    void testImmutable() throws Exception {
        var node = parseJson("""
                {"name": "test"}
                """);

        var provider = new ConfigurationPropertyProvider(node);
        assertFalse(provider.isMutable());
        assertThrows(DiException.class, () -> provider.setProperty("name", "changed"));
    }

    @Test
    void testCopy() throws Exception {
        var node = parseJson("""
                {"name": "test"}
                """);

        var provider = new ConfigurationPropertyProvider(node);
        var copy = provider.copy();
        assertNotNull(copy);
        assertEquals("test", copy.getProperty("name", IClass.getClass(String.class)).orElse(null));
    }

    private IConfigurationNode parseJson(String json) throws ConfigurationException {
        var format = new JsonConfigurationFormat();
        var is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        return format.parse(is);
    }
}
