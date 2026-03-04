package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.dsl.ConfigurationBuilder;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.reflection.JdkReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

class ConfigurationBuilderTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setUpReflection() throws Exception {
        reflectionBuilder = ReflectionBuilder.builder()
            .withProvider(new JdkReflectionProvider());
        reflectionBuilder.build();
    }

    @Test
    void testBuildWithDefaults() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .provide(reflectionBuilder)
                .build();
        assertNotNull(populator);
    }

    @Test
    void testBuildWithCustomFormat() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .provide(reflectionBuilder)
                .withFormat(new JsonConfigurationFormat())
                .build();
        assertNotNull(populator);
    }

    @Test
    void testBuildWithStrategy() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .provide(reflectionBuilder)
                .withMappingStrategy("SMART")
                .build();
        assertNotNull(populator);
    }

    @Test
    void testBuildWithStrict() throws Exception {
        var populator = ConfigurationBuilder.builder()
                .provide(reflectionBuilder)
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
                .provide(reflectionBuilder)
                .withFormat(new JsonConfigurationFormat())
                .withMappingStrategy("SMART")
                .strict(false)
                .build();

        assertNotNull(populator);
    }
}
