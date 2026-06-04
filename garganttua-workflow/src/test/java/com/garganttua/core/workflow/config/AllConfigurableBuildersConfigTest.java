package com.garganttua.core.workflow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.classloader.IClassLoaderManager;
import com.garganttua.core.classloader.dsl.ClassLoaderManagerBuilder;
import com.garganttua.core.configuration.binding.ConfigurationApplier;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.dsl.MutexManagerBuilder;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.dsl.ObservabilityBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.script.dsl.ScriptsBuilder;
import com.garganttua.core.workflow.dsl.WorkflowsBuilder;

/**
 * Cross-cutting proof that <em>every</em> {@code @ConfigurableBuilder} across all modules is
 * actually driven by a configuration file through the shared {@link BuilderPopulator}. Hosted in
 * {@code garganttua-workflow} because its test classpath transitively reaches all eight builder
 * modules (injection, expression, runtime, script, workflow, mutex, observability, classloader).
 *
 * <p>For the six {@code IPackageableBuilder} builders we assert that {@code withPackages} from
 * config lands in {@code getPackages()} (proves the real builder's scalar setters are reached).
 * The standalone-buildable ones (mutex, observability, classloader) are also built to assert the
 * configured builder produces a live object. Injection is covered via its property path. Arbitrary
 * nesting depth and the keyed-child / keyed-scalar / no-arg shapes are proven separately by
 * {@code DeepNestingPopulatorTest} and {@code ConfigDeclares{Injection,Qualified}*Test}.</p>
 */
@DisplayName("Every @ConfigurableBuilder is driven by config (all modules)")
class AllConfigurableBuildersConfigTest {

    @BeforeAll
    static void installReflection() throws Exception {
        ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner())
                .build();
    }

    private static ConfigurationApplier applier() {
        return new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false));
    }

    private static StringConfigurationSource src(String json) {
        return new StringConfigurationSource(json, "json");
    }

    /** Config setting two packages (and disabling scan) under the given builder alias. */
    private static String packagesJson(String module) {
        return "{ \"$module\": \"" + module + "\","
                + " \"autoDetect\": false,"
                + " \"withPackages\": [\"com.test.alpha\", \"com.test.beta\"] }";
    }

    private void assertPackagesConfigured(IBuilder<?> builder, String module, Supplier<String[]> getPackages)
            throws Exception {
        applier().apply(builder, src(packagesJson(module)));
        var pkgs = Arrays.asList(getPackages.get());
        assertTrue(pkgs.containsAll(List.of("com.test.alpha", "com.test.beta")),
                () -> module + " builder did not pick up packages from config; got " + pkgs);
    }

    // ---- the six IPackageableBuilder builders: scalar setters reached from config ----

    @Test
    void runtimesBuilderDrivenByConfig() throws Exception {
        var b = RuntimesBuilder.builder();
        assertPackagesConfigured(b, "runtimes", b::getPackages);
    }

    @Test
    void scriptsBuilderDrivenByConfig() throws Exception {
        var b = ScriptsBuilder.builder();
        assertPackagesConfigured(b, "scripts", b::getPackages);
    }

    @Test
    void workflowsBuilderDrivenByConfig() throws Exception {
        var b = WorkflowsBuilder.builder();
        assertPackagesConfigured(b, "workflows", b::getPackages);
    }

    @Test
    void expressionBuilderDrivenByConfig() throws Exception {
        var b = ExpressionContextBuilder.builder();
        assertPackagesConfigured(b, "expression", b::getPackages);
    }

    @Test
    void mutexBuilderDrivenByConfigAndBuilds() throws Exception {
        var b = MutexManagerBuilder.builder();
        assertPackagesConfigured(b, "mutex", b::getPackages);
        IMutexManager mgr = b.build();
        assertNotNull(mgr, "configured mutex manager must build");
    }

    @Test
    void observabilityBuilderDrivenByConfigAndBuilds() throws Exception {
        var b = ObservabilityBuilder.create();
        assertPackagesConfigured(b, "observability", b::getPackages);
        ObservabilityBinding binding = b.build();
        assertNotNull(binding, "configured observability binding must build");
    }

    // ---- classloader: no configurable surface — a config file applies cleanly and still builds ----

    @Test
    @DisplayName("classloader builder has no settable keys; a config file applies and builds anyway")
    void classloaderBuilderToleratesConfigAndBuilds() throws Exception {
        var b = ClassLoaderManagerBuilder.builder();
        // lax mode: an unknown key is ignored, not fatal
        applier().apply(b, src("{ \"$module\": \"classloader\", \"someUnknownKey\": \"x\" }"));
        IClassLoaderManager mgr = b.build();
        assertNotNull(mgr, "classloader manager must build after applying a (no-op) config");
    }

    // ---- injection: property set from config, resolvable from the built context ----

    @Test
    void injectionBuilderPropertiesDrivenByConfig() throws Exception {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build();

        IInjectionContextBuilder b = InjectionContext.builder().provide(rb);
        applier().apply(b, src("{ \"$module\": \"injection\","
                + " \"propertyProvider\": { \"garganttua\": { \"withProperty\": {"
                + "   \"app.name\": \"from-config\" } } } }"));

        IInjectionContext ctx = b.build();
        ctx.onInit().onStart();
        assertEquals("from-config",
                ctx.getProperty("app.name", IClass.getClass(String.class)).orElse(null),
                "injection property declared from config must resolve");
    }
}
