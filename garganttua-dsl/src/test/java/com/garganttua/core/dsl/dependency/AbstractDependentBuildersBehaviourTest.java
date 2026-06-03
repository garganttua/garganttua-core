package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
 * Behaviour tests for the four dependent builder base classes:
 * {@link AbstractDependentBuilder}, {@link AbstractLinkedDependentBuilder},
 * {@link AbstractAutomaticDependentBuilder} and
 * {@link AbstractAutomaticLinkedDependentBuilder}.
 *
 * <p>Focus: the build template phase ordering (auto-detect → pre-build → build
 * → post-build), result caching, {@code @DependsOn}/spec merge, provide()
 * delegation, linked navigation, and exception propagation — areas not covered
 * by the existing {@code DependentBuilderSupport*Test} classes which test the
 * support helper in isolation.</p>
 */
@DisplayName("DSL dependent builder bases behaviour")
class AbstractDependentBuildersBehaviourTest {

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

    // ---- upstream dependency builders ------------------------------------

    static class DepA implements IObservableBuilder<DepA, String> {
        @Override
        public String build() {
            return "A-built";
        }

        @Override
        public DepA observer(IBuilderObserver<DepA, String> observer) {
            return this;
        }
    }

    static class DepB implements IObservableBuilder<DepB, Integer> {
        @Override
        public Integer build() {
            return 5;
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

    // ====================================================================
    // AbstractDependentBuilder
    // ====================================================================

    static class PlainDependent extends AbstractDependentBuilder<PlainDependent, String> {
        final List<String> order = new ArrayList<>();
        boolean failBuild = false;

        PlainDependent(Set<DependencySpec> deps) {
            super(deps);
        }

        @Override
        protected String doBuild() throws DslException {
            order.add("build");
            if (failBuild) {
                throw new DslException("plain-fail");
            }
            return "result";
        }

        @Override
        protected void doPreBuildWithDependency(Object dependency) {
            order.add("pre:" + dependency);
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
            order.add("post:" + dependency);
        }
    }

    @Test
    @DisplayName("build runs pre-build, then build, then post-build in order")
    void plainPhaseOrder() throws DslException {
        PlainDependent b = new PlainDependent(Set.of(DependencySpec.require(depA())));
        b.provide(new DepA());
        assertEquals("result", b.build());
        assertEquals(List.of("pre:A-built", "build", "post:A-built"), b.order);
    }

    @Test
    @DisplayName("build caches the result, doBuild runs only once")
    void plainCaches() throws DslException {
        PlainDependent b = new PlainDependent(Set.of());
        String r1 = b.build();
        String r2 = b.build();
        assertSame(r1, r2);
        assertEquals(1, b.order.stream().filter("build"::equals).count());
    }

    @Test
    @DisplayName("required dependency never provided throws on pre-build phase")
    void plainRequiredMissingThrows() {
        PlainDependent b = new PlainDependent(Set.of(DependencySpec.require(depA())));
        DslException ex = assertThrows(DslException.class, b::build);
        assertTrue(ex.getMessage().contains("BUILD") || ex.getMessage().toLowerCase().contains("requir"),
                "message should mention the failing required BUILD dependency: " + ex.getMessage());
    }

    @Test
    @DisplayName("provide rejecting undeclared dependency surfaces DslException")
    void plainProvideUndeclared() {
        PlainDependent b = new PlainDependent(Set.of(DependencySpec.use(depA())));
        assertThrows(DslException.class, () -> b.provide(new DepB()));
    }

    @Test
    @DisplayName("provide returns this for chaining")
    void plainProvideReturnsThis() throws DslException {
        PlainDependent b = new PlainDependent(Set.of(DependencySpec.use(depA())));
        assertSame(b, b.provide(new DepA()));
    }

    @Test
    @DisplayName("doBuild failure propagates and post-build never runs")
    void plainBuildFailureNoPost() {
        PlainDependent b = new PlainDependent(Set.of(DependencySpec.require(depA())));
        try {
            b.provide(new DepA());
        } catch (DslException e) {
            throw new AssertionError(e);
        }
        b.failBuild = true;
        assertThrows(DslException.class, b::build);
        assertFalse(b.order.stream().anyMatch(s -> s.startsWith("post:")),
                "post-build must not run when doBuild fails");
    }

    @Test
    @DisplayName("use()/require() reflect the declared spec classification")
    void plainUseRequireClassification() {
        PlainDependent b = new PlainDependent(Set.of(
                DependencySpec.require(depA()),
                DependencySpec.use(depB())));
        assertTrue(b.require().contains(depA()));
        assertTrue(b.use().contains(depB()));
        assertEquals(1, b.require().size());
        assertEquals(1, b.use().size());
    }

    @Test
    @DisplayName("@DependsOn annotation specs merge with constructor specs")
    void plainAnnotationMerge() {
        AnnotatedDependent b = new AnnotatedDependent();
        // The @DependsOn(require DepA) plus constructor use(DepB)
        assertTrue(b.require().contains(depA()), "annotation-declared require should appear");
        assertTrue(b.use().contains(depB()), "constructor-declared use should appear");
    }

    @DependsOn(target = DepA.class,
            stage = DependencyStage.BUILD,
            kind = DependencyKind.BUILT,
            requirement = DependencyRequirement.REQUIRED)
    static class AnnotatedDependent extends AbstractDependentBuilder<AnnotatedDependent, String> {
        AnnotatedDependent() {
            super(Set.of(DependencySpec.use(depB())));
        }

        @Override
        protected String doBuild() {
            return "x";
        }

        @Override
        protected void doPreBuildWithDependency(Object dependency) {
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
        }
    }

    @Test
    @DisplayName("runConfigurationStage fires the configure hook for a provided config dep")
    void plainRunConfigurationStage() throws DslException {
        List<Object> configured = new ArrayList<>();
        PlainDependent b = new PlainDependent(Set.of(DependencySpec.configure(depA()))) {
            @Override
            protected void doConfigureWithDependencyBuilder(IObservableBuilder<?, ?> dependencyBuilder) {
                configured.add(dependencyBuilder);
            }
        };
        DepA up = new DepA();
        b.provide(up);
        b.runConfigurationStage();
        assertEquals(1, configured.size());
        assertSame(up, configured.get(0));
    }

    // ====================================================================
    // AbstractLinkedDependentBuilder
    // ====================================================================

    static class LinkedDependent extends AbstractLinkedDependentBuilder<LinkedDependent, String, String> {
        final List<String> order = new ArrayList<>();

        LinkedDependent(String link, Set<DependencySpec> deps) {
            super(link, deps);
        }

        @Override
        protected String doBuild() {
            order.add("build");
            return "linked-result";
        }

        @Override
        protected void doPreBuildWithDependency(Object dependency) {
            order.add("pre:" + dependency);
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
            order.add("post:" + dependency);
        }
    }

    @Test
    @DisplayName("LinkedDependent.up() returns the parent link")
    void linkedDependentUp() {
        LinkedDependent b = new LinkedDependent("parent", Set.of());
        assertEquals("parent", b.up());
    }

    @Test
    @DisplayName("LinkedDependent constructor rejects null link")
    void linkedDependentNullLink() {
        assertThrows(NullPointerException.class, () -> new LinkedDependent(null, Set.of()));
    }

    @Test
    @DisplayName("LinkedDependent phase order with a provided build dependency")
    void linkedDependentPhaseOrder() throws DslException {
        LinkedDependent b = new LinkedDependent("p", Set.of(DependencySpec.use(depA())));
        b.provide(new DepA());
        assertEquals("linked-result", b.build());
        assertEquals(List.of("pre:A-built", "build", "post:A-built"), b.order);
    }

    @Test
    @DisplayName("LinkedDependent.setUp (void) re-parents the builder")
    void linkedDependentSetUp() {
        LinkedDependent b = new LinkedDependent("p1", Set.of());
        b.setUp("p2");
        assertEquals("p2", b.up());
    }

    // ====================================================================
    // AbstractAutomaticDependentBuilder
    // ====================================================================

    static class AutoDependent extends AbstractAutomaticDependentBuilder<AutoDependent, String> {
        final List<String> order = new ArrayList<>();

        AutoDependent(Set<DependencySpec> deps) {
            super(deps);
        }

        @Override
        protected String doBuild() {
            order.add("build");
            return "auto-result";
        }

        @Override
        protected void doAutoDetection() {
            order.add("autodetect");
        }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) {
            order.add("autodetectdep:" + dependency);
        }

        @Override
        protected void doPreBuildWithDependency(Object dependency) {
            order.add("pre:" + dependency);
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
            order.add("post:" + dependency);
        }
    }

    @Test
    @DisplayName("AutoDependent with auto-detect disabled skips both auto-detect phases")
    void autoDependentNoAutoDetect() throws DslException {
        AutoDependent b = new AutoDependent(Set.of(DependencySpec.use(depA())));
        b.provide(new DepA());
        b.build();
        assertEquals(List.of("pre:A-built", "build", "post:A-built"), b.order,
                "no autodetect entries when auto-detect is disabled");
    }

    @Test
    @DisplayName("AutoDependent with auto-detect runs full 5-phase sequence in order")
    void autoDependentFullSequence() throws DslException {
        AutoDependent b = new AutoDependent(Set.of(DependencySpec.autoDetect(depA())));
        b.autoDetect(true);
        b.provide(new DepA());
        b.build();
        assertEquals(
                List.of("autodetect", "autodetectdep:A-built", "build"),
                b.order.subList(0, 3),
                "ordering: doAutoDetection, then autodetect-with-dep, then build");
    }

    @Test
    @DisplayName("AutoDependent caches; doBuild runs once across multiple build() calls")
    void autoDependentCaches() throws DslException {
        AutoDependent b = new AutoDependent(Set.of());
        b.build();
        b.build();
        assertEquals(1, b.order.stream().filter("build"::equals).count());
    }

    @Test
    @DisplayName("AutoDependent inherits invalidate/isInvalidated from AbstractAutomaticBuilder")
    void autoDependentInvalidate() {
        AutoDependent b = new AutoDependent(Set.of());
        assertFalse(b.isInvalidated());
        b.invalidate();
        assertTrue(b.isInvalidated());
    }

    // ====================================================================
    // AbstractAutomaticLinkedDependentBuilder
    // ====================================================================

    static class AutoLinkedDependent
            extends AbstractAutomaticLinkedDependentBuilder<AutoLinkedDependent, String, String> {
        final List<String> order = new ArrayList<>();

        AutoLinkedDependent(String link, Set<DependencySpec> deps) {
            super(link, deps);
        }

        @Override
        protected String doBuild() {
            order.add("build");
            return "auto-linked-result";
        }

        @Override
        protected void doAutoDetection() {
            order.add("autodetect");
        }

        @Override
        protected void doAutoDetectionWithDependency(Object dependency) {
            order.add("autodetectdep:" + dependency);
        }

        @Override
        protected void doPreBuildWithDependency(Object dependency) {
            order.add("pre:" + dependency);
        }

        @Override
        protected void doPostBuildWithDependency(Object dependency) {
            order.add("post:" + dependency);
        }
    }

    @Test
    @DisplayName("AutoLinkedDependent.up() returns the link and constructor rejects null")
    void autoLinkedDependentUp() {
        AutoLinkedDependent b = new AutoLinkedDependent("L", Set.of());
        assertEquals("L", b.up());
        assertThrows(NullPointerException.class, () -> new AutoLinkedDependent(null, Set.of()));
    }

    @Test
    @DisplayName("AutoLinkedDependent.setUp re-parents and returns this")
    void autoLinkedDependentSetUp() {
        AutoLinkedDependent b = new AutoLinkedDependent("L1", Set.of());
        assertSame(b, b.setUp("L2"));
        assertEquals("L2", b.up());
    }

    @Test
    @DisplayName("AutoLinkedDependent full phase order with auto-detect enabled")
    void autoLinkedDependentFullSequence() throws DslException {
        AutoLinkedDependent b = new AutoLinkedDependent("L", Set.of(DependencySpec.use(depA())));
        b.autoDetect(true);
        b.provide(new DepA());
        b.build();
        assertEquals(
                List.of("autodetect", "pre:A-built", "build", "post:A-built"),
                b.order,
                "build dep is BUILT-kind in BUILD stage, so it appears in pre/post not autodetect");
    }

    @Test
    @DisplayName("AutoLinkedDependent caches the built result")
    void autoLinkedDependentCaches() throws DslException {
        AutoLinkedDependent b = new AutoLinkedDependent("L", Set.of());
        String r1 = b.build();
        String r2 = b.build();
        assertSame(r1, r2);
        assertEquals(1, b.order.stream().filter("build"::equals).count());
    }
}
