package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.dsl.ConfigurationBuilder;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;

class ConfigurationBuilderTest {

    @Test
    void testBuildWithDefaults() throws Exception {
        var populator = ConfigurationBuilder.builder().build();
        assertNotNull(populator);
    }

    @Test
    void testBuildWithCustomFormat() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .withFormat(new JsonConfigurationFormat())
                .build();
        assertNotNull(populator);
    }

    @Test
    void testBuildWithStrategy() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .withMappingStrategy("SMART")
                .build();
        assertNotNull(populator);
    }

    @Test
    void testBuildWithStrict() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .strict(true)
                .build();
        assertNotNull(populator);
    }

    @Test
    void testSourceBuilder() throws Exception {
        var builder = ConfigurationBuilder.builder();
        var sourceBuilder = builder.source();
        assertNotNull(sourceBuilder);

        var parentBuilder = sourceBuilder.classpath("test-config.json").up();
        assertSame(builder, parentBuilder);
    }

    @Test
    void testFullDsl() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .withFormat(new JsonConfigurationFormat())
                .withMappingStrategy("SMART")
                .strict(false)
                .build();

        assertNotNull(populator);
    }
}
