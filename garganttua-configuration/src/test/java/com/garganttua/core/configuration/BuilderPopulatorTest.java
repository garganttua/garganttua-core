package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;

import lombok.Getter;

class BuilderPopulatorTest {

    private BuilderPopulator populator;

    @BeforeEach
    void setUp() {
        this.populator = new BuilderPopulator(
                List.of(new JsonConfigurationFormat()),
                MethodMappingStrategy.SMART,
                false);
    }

    // Simple test builder
    @Getter
    public static class SimpleBuilder implements IBuilder<String> {
        private String name;
        private int port;
        private boolean debug;

        public SimpleBuilder name(String name) { this.name = name; return this; }
        public SimpleBuilder withPort(int port) { this.port = port; return this; }
        public SimpleBuilder debug(boolean debug) { this.debug = debug; return this; }

        @Override
        public String build() throws DslException { return this.name + ":" + this.port; }
    }

    @Test
    void testFlatPopulation() throws Exception {
        var json = """
                {"name": "myApp", "port": 8080, "debug": true}
                """;
        var source = new StringConfigurationSource(json, "json");
        var builder = new SimpleBuilder();

        this.populator.populate(builder, source);

        assertEquals("myApp", builder.getName());
        assertEquals(8080, builder.getPort());
        assertTrue(builder.isDebug());
    }

    @Test
    void testUnknownKeyLaxMode() throws Exception {
        var json = """
                {"name": "myApp", "unknown": "value"}
                """;
        var source = new StringConfigurationSource(json, "json");
        var builder = new SimpleBuilder();

        // Should not throw in lax mode
        assertDoesNotThrow(() -> this.populator.populate(builder, source));
        assertEquals("myApp", builder.getName());
    }

    @Test
    void testUnknownKeyStrictMode() throws Exception {
        var strictPopulator = new BuilderPopulator(
                List.of(new JsonConfigurationFormat()),
                MethodMappingStrategy.SMART,
                true);
        var json = """
                {"name": "myApp", "unknown": "value"}
                """;
        var source = new StringConfigurationSource(json, "json");
        var builder = new SimpleBuilder();

        assertThrows(ConfigurationException.class, () -> strictPopulator.populate(builder, source));
    }

    // Builder with array support
    @Getter
    public static class ArrayBuilder implements IBuilder<String> {
        private final java.util.List<String> tags = new java.util.ArrayList<>();

        public ArrayBuilder tags(String tag) { this.tags.add(tag); return this; }

        @Override
        public String build() throws DslException { return String.join(",", this.tags); }
    }

    @Test
    void testArrayRepeatedCalls() throws Exception {
        var json = """
                {"tags": ["web", "api", "rest"]}
                """;
        var source = new StringConfigurationSource(json, "json");
        var builder = new ArrayBuilder();

        this.populator.populate(builder, source);

        assertEquals(3, builder.getTags().size());
        assertEquals("web", builder.getTags().get(0));
        assertEquals("api", builder.getTags().get(1));
        assertEquals("rest", builder.getTags().get(2));
    }

    @Test
    void testPopulateFromNode() throws Exception {
        var format = new JsonConfigurationFormat();
        var json = """
                {"name": "fromNode", "port": 9090}
                """;
        var source = new StringConfigurationSource(json, "json");
        var node = format.parse(source.getInputStream());
        var builder = new SimpleBuilder();

        this.populator.populate(builder, node);

        assertEquals("fromNode", builder.getName());
        assertEquals(9090, builder.getPort());
    }
}
