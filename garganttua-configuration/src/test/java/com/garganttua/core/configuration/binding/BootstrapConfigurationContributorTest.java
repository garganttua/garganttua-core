package com.garganttua.core.configuration.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.IBuilder;

/**
 * Verifies the bootstrap CONFIGURATION-stage contributor: it discovers a classpath
 * configuration file ({@code garganttua/config/demo.json}, shebang {@code $module:demo}),
 * matches it to the {@code @ConfigurableBuilder("demo")} instance among the supplied
 * builders, and applies it — leaving non-matching builders untouched.
 */
class BootstrapConfigurationContributorTest {

    @Test
    void appliesDiscoveredConfigToTheMatchingConfigurableBuilder() {
        DemoConfigurableBuilder demo = new DemoConfigurableBuilder();
        UnrelatedBuilder other = new UnrelatedBuilder();

        new BootstrapConfigurationContributor().contribute(List.of(demo, other));

        // demo.json -> name=booted, count=3, enabled=true
        assertEquals("booted:3:true", demo.build());
        // a builder without @ConfigurableBuilder is left untouched
        assertEquals("untouched", other.build());
    }

    @Test
    void noMatchingBuilderIsANoOp() {
        UnrelatedBuilder other = new UnrelatedBuilder();
        new BootstrapConfigurationContributor().contribute(List.of(other));
        assertEquals("untouched", other.build());
    }

    /** A builder with no {@code @ConfigurableBuilder} marker — must never be configured. */
    static final class UnrelatedBuilder implements IBuilder<String> {
        @Override
        public String build() {
            return "untouched";
        }
    }
}
