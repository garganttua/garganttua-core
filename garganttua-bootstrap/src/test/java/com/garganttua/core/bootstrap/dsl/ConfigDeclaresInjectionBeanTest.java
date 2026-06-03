package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.binding.ConfigurationApplier;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Real injection end-to-end: a bean declared <em>only</em> from a configuration file —
 * {@code beanProvider.garganttua.withBean.<FQN>.strategy=singleton} — is registered into the
 * default provider and resolvable from the built {@link IInjectionContext}. Exercises the
 * keyed child-builder traversal against the real injection DSL (beanProvider(String) →
 * withBean(IClass) → strategy(BeanStrategy)).
 */
@DisplayName("Declare an injection bean from a configuration file")
class ConfigDeclaresInjectionBeanTest {

    @Test
    void beanDeclaredInConfigIsResolvableFromContext() throws Exception {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build(); // installs the global IReflection (IClass.setReflection)

        IInjectionContextBuilder builder = InjectionContext.builder().provide(rb);

        String json = "{"
                + "\"$module\": \"injection\","
                + "\"beanProvider\": {"
                + "  \"garganttua\": {"
                + "    \"withBean\": {"
                + "      \"" + ConfigDeclaredBean.class.getName() + "\": { \"strategy\": \"singleton\" }"
                + "    }"
                + "  }"
                + "}}";

        new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false))
                .apply(builder, new StringConfigurationSource(json, "json"));

        IInjectionContext ctx = builder.build();
        ctx.onInit().onStart();

        BeanReference<ConfigDeclaredBean> ref = new BeanReference<>(
                IClass.getClass(ConfigDeclaredBean.class), Optional.empty(), Optional.empty(), Set.of());

        assertTrue(ctx.queryBean(ref).isPresent(),
                "bean declared from config must be resolvable in the built injection context");
    }

    @Test
    @DisplayName("config can declare a bean in a brand-new provider scope (create-if-absent)")
    void beanDeclaredInNewScopeIsResolvable() throws Exception {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build();

        IInjectionContextBuilder builder = InjectionContext.builder().provide(rb);

        String json = "{"
                + "\"$module\": \"injection\","
                + "\"beanProvider\": { \"application\": { \"withBean\": {"
                + "  \"" + ConfigDeclaredBean.class.getName() + "\": { \"strategy\": \"prototype\" } } } }"
                + "}";

        new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false))
                .apply(builder, new StringConfigurationSource(json, "json"));

        IInjectionContext ctx = builder.build();
        ctx.onInit().onStart();

        BeanReference<ConfigDeclaredBean> ref = new BeanReference<>(
                IClass.getClass(ConfigDeclaredBean.class), Optional.empty(), Optional.empty(), Set.of());

        assertTrue(ctx.queryBean(ref).isPresent(), "bean must resolve");
        assertTrue(ctx.queryBean("application", ref).isPresent(),
                "bean must resolve from the newly-created 'application' provider scope");
    }

    @Test
    @DisplayName("an unknown bean class in config fails clearly")
    void unknownBeanClassFailsClearly() throws Exception {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build();

        IInjectionContextBuilder builder = InjectionContext.builder().provide(rb);
        String json = "{ \"$module\": \"injection\", \"beanProvider\": { \"garganttua\": {"
                + " \"withBean\": { \"com.example.DoesNotExist\": { \"strategy\": \"singleton\" } } } } }";

        ConfigurationApplier applier = new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false));

        assertThrows(Exception.class,
                () -> applier.apply(builder, new StringConfigurationSource(json, "json")));
    }
}
