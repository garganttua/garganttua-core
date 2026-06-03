package com.garganttua.core.configuration.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;

/**
 * End-to-end: a shebang-tagged config source resolves its target alias and populates
 * a {@code @ConfigurableBuilder} via the applier — with the reserved {@code $module}
 * key ignored (not mistaken for a builder method).
 */
class ConfigurationApplierTest {

    private static ConfigurationApplier applier() {
        return new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false));
    }

    @Test
    void jsonWithModuleKeyPopulatesBuilderAndIgnoresShebang() throws Exception {
        String json = "{ \"$module\": \"demo\", \"name\": \"svc\", \"count\": 7, \"enabled\": true }";
        IConfigurationSource source = new StringConfigurationSource(json, "json");

        DemoConfigurableBuilder builder = applier().apply(new DemoConfigurableBuilder(), source);

        assertEquals("svc:7:true", builder.build());
    }

    @Test
    void targetAliasIsReadFromTheModuleKey() throws Exception {
        IConfigurationSource source =
                new StringConfigurationSource("{ \"$module\": \"demo\", \"name\": \"x\" }", "json");
        assertEquals(Optional.of("demo"), applier().targetAlias(source));
    }

    @Test
    void configWithoutModuleKeyStillPopulates() throws Exception {
        IConfigurationSource source =
                new StringConfigurationSource("{ \"name\": \"plain\", \"count\": 1 }", "json");
        DemoConfigurableBuilder builder = applier().apply(new DemoConfigurableBuilder(), source);
        assertEquals("plain:1:false", builder.build());
        assertTrue(applier().targetAlias(source).isEmpty());
    }
}
