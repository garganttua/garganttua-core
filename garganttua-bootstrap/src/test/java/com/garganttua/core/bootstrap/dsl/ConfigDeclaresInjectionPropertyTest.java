package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.binding.ConfigurationApplier;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Real injection end-to-end: properties declared <em>only</em> from a configuration file —
 * {@code propertyProvider.<scope>.withProperty.<key>=<value>} — are registered into the
 * property provider and resolvable from the built {@link IInjectionContext}. Exercises the
 * keyed-scalar traversal against the real injection DSL (propertyProvider(String) ->
 * withProperty(String, String)), with on-read coercion to typed values.
 */
@DisplayName("Declare injection properties from a configuration file")
class ConfigDeclaresInjectionPropertyTest {

    private static IInjectionContextBuilder applied(String json) throws Exception {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build(); // installs the global IReflection (IClass.setReflection)

        IInjectionContextBuilder builder = InjectionContext.builder().provide(rb);
        new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false))
                .apply(builder, new StringConfigurationSource(json, "json"));
        return builder;
    }

    @Test
    void propertiesDeclaredInConfigAreResolvableFromContext() throws Exception {
        String json = "{"
                + "\"$module\": \"injection\","
                + "\"propertyProvider\": {"
                + "  \"garganttua\": {"
                + "    \"withProperty\": {"
                + "      \"database.host\": \"localhost\","
                + "      \"app.port\": \"8080\","
                + "      \"app.enabled\": \"true\""
                + "    }"
                + "  }"
                + "}}";

        IInjectionContext ctx = applied(json).build();
        ctx.onInit().onStart();

        assertEquals("localhost",
                ctx.getProperty("database.host", IClass.getClass(String.class)).orElse(null),
                "string property declared from config must resolve");
        // on-read coercion: the value is stored as text, read as a typed scalar
        assertEquals(8080,
                ctx.getProperty("app.port", IClass.getClass(Integer.class)).orElse(null),
                "property must coerce to Integer on read");
        assertTrue(ctx.getProperty("app.enabled", IClass.getClass(Boolean.class)).orElse(false),
                "property must coerce to Boolean on read");
    }

    @Test
    @DisplayName("config can declare properties in a brand-new provider scope (create-if-absent)")
    void propertiesDeclaredInNewScopeAreResolvable() throws Exception {
        String json = "{"
                + "\"$module\": \"injection\","
                + "\"propertyProvider\": { \"application\": { \"withProperty\": {"
                + "  \"service.url\": \"https://example.org\" } } }"
                + "}";

        IInjectionContext ctx = applied(json).build();
        ctx.onInit().onStart();

        assertEquals("https://example.org",
                ctx.getProperty("service.url", IClass.getClass(String.class)).orElse(null),
                "property declared in a new scope must resolve");
        assertEquals("https://example.org",
                ctx.getProperty("application", "service.url", IClass.getClass(String.class)).orElse(null),
                "property must resolve from the newly-created 'application' provider scope");
    }
}
