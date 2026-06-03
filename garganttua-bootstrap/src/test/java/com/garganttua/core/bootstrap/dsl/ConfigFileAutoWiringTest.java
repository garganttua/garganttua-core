package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * End-to-end: a classpath configuration file ({@code garganttua/config/e2e.json}, shebang
 * {@code "$module":"e2e"}) is auto-discovered and applied to the {@code @ConfigurableBuilder("e2e")}
 * builder by the garganttua-configuration contributor, invoked by Bootstrap at the CONFIGURATION
 * stage — proving the SPI wiring end to end with a real Bootstrap run.
 */
@DisplayName("Config-file auto-wiring through Bootstrap CONFIGURATION stage")
class ConfigFileAutoWiringTest {

    @BeforeEach
    void wireReflection() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0)
                .build());
    }

    @AfterEach
    void resetReflection() {
        IClass.setReflection(null);
    }

    @Test
    @DisplayName("config file configures the matching @ConfigurableBuilder before it builds")
    void configFileAutoConfiguresBuilder() throws DslException {
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0);

        ConfigurableBootstrapBuilder builder = new ConfigurableBootstrapBuilder();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.autoDetect(false);
        bootstrap.disableSpiFallback();
        bootstrap.provide(reflectionBuilder);
        bootstrap.withBuilder(builder);

        bootstrap.build();

        // The contributor discovered e2e.json, matched alias "e2e", and applied it.
        assertEquals("fromfile", builder.appliedName());
        assertEquals(9, builder.appliedCount());
    }
}
