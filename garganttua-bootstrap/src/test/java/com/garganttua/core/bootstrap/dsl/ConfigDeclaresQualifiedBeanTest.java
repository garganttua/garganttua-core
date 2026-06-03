package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
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
 * Verifies a bean declared from config WITH a qualifier — {@code withBean.<FQN>.qualifier=<QualifierFQN>}
 * — carries that qualifier in the built context: a query bearing the qualifier resolves it, a query
 * bearing a different qualifier does not, and an unqualified query still resolves it.
 */
@DisplayName("Declare a qualified injection bean from a configuration file")
class ConfigDeclaresQualifiedBeanTest {

    @Test
    void qualifiedBeanDeclaredFromConfigMatchesOnlyItsQualifier() throws Exception {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build();

        IInjectionContextBuilder builder = InjectionContext.builder().provide(rb);

        String json = "{"
                + "\"$module\": \"injection\","
                + "\"beanProvider\": { \"app\": { \"withBean\": { \"" + ConfigDeclaredBean.class.getName() + "\": {"
                + "  \"strategy\": \"singleton\","
                + "  \"qualifier\": \"" + ConfigPrimary.class.getName() + "\""
                + "} } } } }";

        new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false))
                .apply(builder, new StringConfigurationSource(json, "json"));

        IInjectionContext ctx = builder.build();
        ctx.onInit().onStart();

        assertTrue(ctx.queryBean(refWith()).isPresent(),
                "unqualified query must resolve the bean");
        assertTrue(ctx.queryBean(refWith(ConfigPrimary.class)).isPresent(),
                "query with the declared qualifier must resolve the bean");
        assertFalse(ctx.queryBean(refWith(ConfigSecondary.class)).isPresent(),
                "query with a different qualifier must NOT resolve the bean");
    }

    @SafeVarargs
    private static BeanReference<ConfigDeclaredBean> refWith(Class<? extends Annotation>... qualifiers) {
        Set<IClass<? extends Annotation>> q = java.util.Arrays.stream(qualifiers)
                .<IClass<? extends Annotation>>map(IClass::getClass)
                .collect(java.util.stream.Collectors.toSet());
        return new BeanReference<>(IClass.getClass(ConfigDeclaredBean.class), Optional.empty(), Optional.empty(), q);
    }
}
