package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Verifies Bootstrap's centralized CONFIGURATION stage:
 *
 * <ul>
 *   <li>A consumer that declares a {@code CONFIGURATION + BUILDER} dep
 *       receives the upstream BUILDER reference BEFORE any builder.build()
 *       runs.</li>
 *   <li>The hook fires at most once per Bootstrap lifetime — idempotent
 *       across rebuild().</li>
 *   <li>Rebuild() does NOT re-fire CONFIGURATION (per the spec).</li>
 * </ul>
 */
@DisplayName("Bootstrap CONFIGURATION stage orchestration")
class BootstrapConfigurationStageTest {

    @BeforeEach
    void wireReflection() throws DslException {
        // DependencySpec helpers use IClass.getClass which reads the global
        // reflection — install it before the fixtures are instantiated.
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0)
                .build());
    }

    @AfterEach
    void resetGlobalReflection() {
        IClass.setReflection(null);
    }

    @Test
    @DisplayName("CONFIGURATION hook fires once and receives the upstream BUILDER")
    void configurationHookFiresBeforeBuilds() throws DslException {
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0);

        UpstreamBuilder upstream = new UpstreamBuilder();
        DownstreamBuilder downstream = new DownstreamBuilder();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.autoDetect(false);
        bootstrap.disableSpiFallback();
        bootstrap.provide(reflectionBuilder);
        bootstrap.withBuilder(upstream);
        bootstrap.withBuilder(downstream);

        bootstrap.build();

        // Configuration hook fired exactly once.
        assertEquals(1, downstream.configurationFireCount.get(),
                "doConfigureWithDependencyBuilder must fire exactly once");
        // And received the actual upstream BUILDER ref (not the built).
        assertNotNull(downstream.receivedUpstreamBuilder);
        assertSame(upstream, downstream.receivedUpstreamBuilder,
                "CONFIGURATION hook must receive the upstream builder ref, not the built object");
        // The upstream was already mutated through the hook (registered in its log)
        assertTrue(upstream.configurationApplied.contains("from-downstream"),
                "downstream must have been able to mutate upstream before its build");
    }

    @Test
    @DisplayName("rebuild() does NOT re-fire CONFIGURATION")
    void rebuildDoesNotRefireConfiguration() throws DslException {
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0);

        UpstreamBuilder upstream = new UpstreamBuilder();
        DownstreamBuilder downstream = new DownstreamBuilder();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.autoDetect(false);
        bootstrap.disableSpiFallback();
        bootstrap.provide(reflectionBuilder);
        bootstrap.withBuilder(upstream);
        bootstrap.withBuilder(downstream);

        bootstrap.build();
        bootstrap.rebuild();

        assertEquals(1, downstream.configurationFireCount.get(),
                "rebuild() must NOT re-fire the CONFIGURATION hook");
    }

    // ---------- Fixtures ------------------------------------------------

    static class UpstreamBuilder
            extends AbstractAutomaticDependentBuilder<UpstreamBuilder, String>
            implements IObservableBuilder<UpstreamBuilder, String> {

        final List<String> configurationApplied = new ArrayList<>();
        private String built;

        UpstreamBuilder() {
            super(Set.of());
        }

        @SuppressWarnings("unused")
        void registerConfig(String tag) {
            this.configurationApplied.add(tag);
        }

        @Override
        public UpstreamBuilder observer(IBuilderObserver<UpstreamBuilder, String> observer) {
            return this;
        }

        @Override
        protected String doBuild() {
            this.built = "upstream-built[" + this.configurationApplied + "]";
            return this.built;
        }

        @Override
        protected void doAutoDetection() { /* no-op */ }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) { /* no-op */ }

        @Override
        protected void doPreBuildWithDependency(Object dependency) { /* no-op */ }

        @Override
        protected void doPostBuildWithDependency(Object dependency) { /* no-op */ }
    }

    static class DownstreamBuilder
            extends AbstractAutomaticDependentBuilder<DownstreamBuilder, String>
            implements IObservableBuilder<DownstreamBuilder, String> {

        final AtomicInteger configurationFireCount = new AtomicInteger();
        IObservableBuilder<?, ?> receivedUpstreamBuilder;

        DownstreamBuilder() {
            super(Set.of(DependencySpec.configure(IClass.getClass(UpstreamBuilder.class))));
        }

        @Override
        public DownstreamBuilder observer(IBuilderObserver<DownstreamBuilder, String> observer) {
            return this;
        }

        @Override
        protected void doConfigureWithDependencyBuilder(IObservableBuilder<?, ?> dependencyBuilder) {
            this.configurationFireCount.incrementAndGet();
            this.receivedUpstreamBuilder = dependencyBuilder;
            // Mutate the upstream while it's still un-built.
            if (dependencyBuilder instanceof UpstreamBuilder ub) {
                ub.registerConfig("from-downstream");
            }
        }

        @Override
        protected String doBuild() { return "downstream-built"; }

        @Override
        protected void doAutoDetection() { /* no-op */ }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) { /* no-op */ }

        @Override
        protected void doPreBuildWithDependency(Object dependency) { /* no-op */ }

        @Override
        protected void doPostBuildWithDependency(Object dependency) { /* no-op */ }
    }
}
