package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link DependentBuilderSupport} focusing on the
 * stage/kind specific iterators (CONFIGURATION, BUILDER-kind), the
 * multi-spec fan-out of {@link DependentBuilderSupport#provide}, and the
 * per-firing idempotency contract — areas not covered by the existing
 * {@code DependentBuilderSupportTest}.
 */
class DependentBuilderSupportBehaviourTest {

    private static final RuntimeReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void setUpReflection() {
        IReflection reflection = (IReflection) Proxy.newProxyInstance(
                IReflection.class.getClassLoader(),
                new Class<?>[]{IReflection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getClass" -> PROVIDER.getClass((Class<?>) args[0]);
                    case "supports" -> PROVIDER.supports((Class<?>) args[0]);
                    case "forName" -> PROVIDER.forName((String) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDownReflection() {
        IClass.setReflection(null);
    }

    static class DepA implements IObservableBuilder<DepA, String> {
        @Override
        public String build() throws DslException {
            return "A";
        }

        @Override
        public DepA observer(IBuilderObserver<DepA, String> observer) {
            return this;
        }
    }

    static class DepB implements IObservableBuilder<DepB, Integer> {
        @Override
        public Integer build() throws DslException {
            return 7;
        }

        @Override
        public DepB observer(IBuilderObserver<DepB, Integer> observer) {
            return this;
        }
    }

    private static IClass<DepA> depA() {
        return IClass.getClass(DepA.class);
    }

    private static IClass<DepB> depB() {
        return IClass.getClass(DepB.class);
    }

    // ---- classification ---------------------------------------------------

    @Test
    @DisplayName("A spec required in any stage classifies under require(), not use()")
    void classificationRequireVsUse() {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.requireAutoDetect(depA()),
                DependencySpec.use(depB())));

        assertEquals(1, support.require().size());
        assertEquals(1, support.use().size());
        assertTrue(support.require().contains(depA()));
        assertTrue(support.use().contains(depB()));
        assertEquals(2, support.getRequireDependencies().size() + support.getUseDependencies().size());
    }

    // ---- provide fan-out --------------------------------------------------

    @Test
    @DisplayName("provide feeds every spec declaring the same upstream class")
    void provideFansOutToAllMatchingSpecs() throws DslException {
        // Same class declared in CONFIGURATION (builder) and BUILD (built).
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.configure(depA()),
                DependencySpec.require(depA())));

        support.provide(new DepA());

        // CONFIGURATION builder-kind hook must see the builder.
        List<IObservableBuilder<?, ?>> configHooked = new ArrayList<>();
        support.processConfigurationDependencies(configHooked::add);
        assertEquals(1, configHooked.size());

        // BUILD built-kind hook must see the built object — proving the
        // second spec also received the reference (otherwise it would throw
        // "not provided").
        List<Object> builtHooked = new ArrayList<>();
        support.processPreBuildDependencies(builtHooked::add);
        assertEquals(List.of("A"), builtHooked);
    }

    @Test
    @DisplayName("provide throws DslException for a class not in the dependency list")
    void provideRejectsUndeclared() {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.use(depA())));
        DslException ex = assertThrows(DslException.class, () -> support.provide(new DepB()));
        assertTrue(ex.getMessage().contains("not declared in the expected dependencies list"));
    }

    @Test
    @DisplayName("provide rejects null dependency with NPE")
    void provideRejectsNull() {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.use(depA())));
        assertThrows(NullPointerException.class, () -> support.provide(null));
    }

    // ---- CONFIGURATION stage ---------------------------------------------

    @Test
    @DisplayName("processConfigurationDependencies throws for a required, unprovided config dep")
    void requiredConfigurationMissing() {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.requireConfigure(depA())));
        DslException ex = assertThrows(DslException.class,
                () -> support.processConfigurationDependencies(b -> {}));
        assertTrue(ex.getMessage().contains("CONFIGURATION"));
    }

    @Test
    @DisplayName("processConfigurationDependencies skips an absent optional config dep")
    void optionalConfigurationAbsentSkipped() throws DslException {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.configure(depA())));
        AtomicInteger calls = new AtomicInteger();
        support.processConfigurationDependencies(b -> calls.incrementAndGet());
        assertEquals(0, calls.get());
    }

    // ---- BUILDER-kind iterators ------------------------------------------

    @Test
    @DisplayName("processPreBuildDependencyBuilders hands the builder reference, not the built object")
    void preBuildBuilderKind() throws DslException {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.useBuilder(depA())));
        DepA upstream = new DepA();
        support.provide(upstream);

        List<IObservableBuilder<?, ?>> seen = new ArrayList<>();
        support.processPreBuildDependencyBuilders(seen::add);
        assertEquals(1, seen.size());
        assertSame(upstream, seen.get(0));
    }

    @Test
    @DisplayName("processAutoDetectionWithDependencyBuilders fires for a provided AUTO_DETECT builder dep")
    void autoDetectBuilderKind() throws DslException {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.autoDetectBuilder(depA())));
        DepA upstream = new DepA();
        support.provide(upstream);

        List<IObservableBuilder<?, ?>> seen = new ArrayList<>();
        support.processAutoDetectionWithDependencyBuilders(seen::add);
        assertEquals(List.of(upstream), seen);
    }

    @Test
    @DisplayName("processPostBuildDependencyBuilders throws for a required, unprovided builder dep")
    void postBuildBuilderRequiredMissing() {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.requireBuilder(depA())));
        assertThrows(DslException.class,
                () -> support.processPostBuildDependencyBuilders(b -> {}));
    }

    // ---- idempotency ------------------------------------------------------

    @Test
    @DisplayName("Pre-build BUILT-kind hook fires at most once across repeated calls")
    void preBuildIdempotent() throws DslException {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.require(depA())));
        support.provide(new DepA());

        AtomicInteger calls = new AtomicInteger();
        support.processPreBuildDependencies(o -> calls.incrementAndGet());
        support.processPreBuildDependencies(o -> calls.incrementAndGet());
        assertEquals(1, calls.get(), "PRE_BUILD must fire only once per dependency");
    }

    @Test
    @DisplayName("Pre and post build are distinct firing events and both fire once")
    void preAndPostAreDistinct() throws DslException {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.require(depA())));
        support.provide(new DepA());

        AtomicInteger pre = new AtomicInteger();
        AtomicInteger post = new AtomicInteger();
        support.processPreBuildDependencies(o -> pre.incrementAndGet());
        support.processPostBuildDependencies(o -> post.incrementAndGet());
        support.processPreBuildDependencies(o -> pre.incrementAndGet());
        support.processPostBuildDependencies(o -> post.incrementAndGet());
        assertEquals(1, pre.get());
        assertEquals(1, post.get());
    }

    @Test
    @DisplayName("Configuration builder dep does not fire in the BUILD-stage iterators")
    void stageIsolationConfigurationVsBuild() throws DslException {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.configure(depA())));
        support.provide(new DepA());

        AtomicInteger buildCalls = new AtomicInteger();
        support.processPreBuildDependencies(o -> buildCalls.incrementAndGet());
        support.processPreBuildDependencyBuilders(b -> buildCalls.incrementAndGet());
        assertEquals(0, buildCalls.get(),
                "a CONFIGURATION-stage dep must not be consumed by BUILD iterators");
    }

    @Test
    @DisplayName("Optional BUILT dep provided but never built throws on pre-build validation")
    void optionalProvidedButUnbuildableThrows() {
        DependentBuilderSupport support = new DependentBuilderSupport(Set.of(
                DependencySpec.use(depA())));
        try {
            // Provide a builder whose build() throws -> validateUseDependency must complain.
            support.provide(new DepA() {
                @Override
                public String build() throws DslException {
                    throw new DslException("cannot build");
                }
            });
        } catch (DslException e) {
            throw new AssertionError("provide should not throw", e);
        }
        DslException ex = assertThrows(DslException.class,
                () -> support.processPreBuildDependencies(o -> {}));
        assertTrue(ex.getMessage().contains("must be built"));
    }
}
