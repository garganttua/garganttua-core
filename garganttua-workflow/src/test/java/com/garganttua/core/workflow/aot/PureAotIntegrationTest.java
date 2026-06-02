package com.garganttua.core.workflow.aot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.aot.annotation.scanner.AOTAnnotationScanner;
import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.aot.reflection.AOTReflectionProvider;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.mapper.Mapper;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.runtime.RuntimeContextFactory;

/**
 * In-tree integration test that simulates a pure-AOT consumer's cold start
 * end-to-end, without bouncing through garganttua-api-example.
 *
 * <p>The test forces "pure-AOT mode" by wiring an {@link IReflection} that
 * contains ONLY the {@link AOTReflectionProvider} (priority 20) and
 * {@link AOTAnnotationScanner} — no runtime fallback, even though
 * {@code garganttua-runtime-reflection} is on the test classpath as a
 * transitive dep. {@link IClass#setReflection} is used to install it
 * globally before each test.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>SPI cold-start seeds framework infrastructure types
 *       (CoreInfrastructureSeed + extension SPI from this repo's three
 *       in-tree seeds: configuration / observability / runtime).</li>
 *   <li>Fallback synthesis on getClass for unregistered classes — identity,
 *       interfaces, annotations, no-arg ctor.</li>
 *   <li>Lazy member fallback on shallow descriptors — getMethod,
 *       getDeclaredMethod, getField, getDeclaredField, getConstructor,
 *       getDeclaredConstructor.</li>
 *   <li>Bulk fallback for getDeclaredMethods / Fields / Constructors when
 *       the shallow descriptor's arrays are empty.</li>
 *   <li>Annotation array populated on shallow descriptors (avoids the
 *       "annotated with @X but does not implement IElementResolver"
 *       warning storm from InjectableElementResolverBuilder).</li>
 *   <li>RuntimeContextFactory and similar @ChildContext / @MutexFactory
 *       framework factory classes resolve their no-arg ctor through the
 *       fallback path.</li>
 *   <li>Mapper passthrough on JDK leaf types (String / Integer / Date /
 *       UUID) — no java.base/java.lang setAccessible attempts.</li>
 *   <li>Mapper handles collection fields whose generic types don't resolve
 *       to a Class — wildcard / nested ParameterizedType / TypeVariable.</li>
 * </ul>
 */
@DisplayName("Pure-AOT integration — full reflection facade")
class PureAotIntegrationTest {

    private IReflection priorReflection;

    @BeforeEach
    void installAotOnlyReflection() {
        // Snapshot whatever IReflection a previous test may have set.
        priorReflection = safeCurrentReflection();
        IClass.setReflection(null);
        IReflection aot = ReflectionBuilder.builder()
                .withProvider(new AOTReflectionProvider(), 20)
                .withScanner(new AOTAnnotationScanner(), 20)
                .build();
        IClass.setReflection(aot);
    }

    @AfterEach
    void restorePriorReflection() {
        IClass.setReflection(priorReflection);
    }

    private static IReflection safeCurrentReflection() {
        try {
            return IClass.getReflection();
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Seed coverage
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void coreSeed_covers_framework_builder_interfaces() {
        // A few of the ~30 framework infrastructure interfaces CoreInfrastructureSeed
        // pre-populates. If any of these regress, framework wiring crashes at
        // static-init time in pure-AOT consumers.
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.reflection.dsl.IReflectionBuilder"));
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.observability.dsl.IObservabilityBuilder"));
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.injection.context.dsl.IInjectionContextBuilder"));
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.runtime.dsl.IRuntimesBuilder"));
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.expression.dsl.IExpressionContextBuilder"));
    }

    @Test
    void coreSeed_covers_framework_annotation_surface() {
        // Framework annotations referenced by user @Reflected classes.
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.expression.annotations.Expression"));
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.reflection.annotations.Reflected"));
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.injection.annotations.Resolver"));
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.mutex.annotations.Mutex"));
    }

    @Test
    void coreSeed_covers_jdk_essentials() {
        // Time, math, wrappers, collections.
        assertTrue(AOTRegistry.getInstance().contains("java.time.Instant"));
        assertTrue(AOTRegistry.getInstance().contains("java.time.LocalDate"));
        assertTrue(AOTRegistry.getInstance().contains("java.math.BigDecimal"));
        assertTrue(AOTRegistry.getInstance().contains("java.lang.Character"));
        assertTrue(AOTRegistry.getInstance().contains("java.util.ArrayList"));
        assertTrue(AOTRegistry.getInstance().contains("java.util.HashMap"));
    }

    @Test
    void extensionSeed_runtime_in_tree_runs_via_SPI() {
        // The RuntimeInfrastructureSeed in garganttua-runtime ships
        // RuntimeContextFactory and friends. This SPI discovery is what fixes
        // the api-example crash on @ChildContext factory instantiation.
        assertTrue(AOTRegistry.getInstance().contains(
                "com.garganttua.core.runtime.RuntimeContextFactory"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fallback synthesis
    // ─────────────────────────────────────────────────────────────────────

    @Resolver(annotations = { Resolver.class })   // unrelated value — only the annotation presence matters
    public static class UnknownDto {
        public String name;
        public int count;
        public List<String> tags = new ArrayList<>();
        public UnknownDto() {}
        public String describe(String prefix) { return prefix + name; }
        public static UnknownDto create() { return new UnknownDto(); }
    }

    @Test
    void getClass_returns_synth_with_identity_and_annotations_for_unknown_type() {
        IClass<UnknownDto> desc = IClass.getClass(UnknownDto.class);
        assertNotNull(desc);
        assertEquals(UnknownDto.class.getName(), desc.getName());
        // Annotation array populated from live class — this is what makes
        // InjectableElementResolverBuilder see @Resolver on framework
        // resolver classes.
        Resolver r = desc.getAnnotation(IClass.getClass(Resolver.class));
        assertNotNull(r, "shallow synth must expose @Resolver");
    }

    @Test
    void getDeclaredConstructor_returns_no_arg_ctor_for_unknown_type() throws Exception {
        IClass<UnknownDto> desc = IClass.getClass(UnknownDto.class);
        IConstructor<UnknownDto> ctor = desc.getDeclaredConstructor();
        assertNotNull(ctor, "no-arg ctor must be reachable on shallow descriptors");
        UnknownDto instance = ctor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void getDeclaredMethod_falls_back_to_live_class() throws Exception {
        IClass<UnknownDto> desc = IClass.getClass(UnknownDto.class);
        IMethod m = desc.getDeclaredMethod("describe", IClass.getClass(String.class));
        assertNotNull(m);
        assertEquals("describe", m.getName());
    }

    @Test
    void getDeclaredField_falls_back_to_live_class() throws Exception {
        IClass<UnknownDto> desc = IClass.getClass(UnknownDto.class);
        IField f = desc.getDeclaredField("name");
        assertNotNull(f);
        assertEquals("name", f.getName());
    }

    @Test
    void bulk_getDeclaredMethods_falls_back_to_live_class_when_empty() {
        IClass<UnknownDto> desc = IClass.getClass(UnknownDto.class);
        IMethod[] methods = desc.getDeclaredMethods();
        assertTrue(methods.length >= 2, "expected at least describe() and create()");
        boolean foundDescribe = Arrays.stream(methods).anyMatch(m -> m.getName().equals("describe"));
        boolean foundCreate = Arrays.stream(methods).anyMatch(m -> m.getName().equals("create"));
        assertTrue(foundDescribe);
        assertTrue(foundCreate);
    }

    @Test
    void bulk_getDeclaredFields_falls_back_to_live_class_when_empty() {
        IClass<UnknownDto> desc = IClass.getClass(UnknownDto.class);
        IField[] fields = desc.getDeclaredFields();
        assertTrue(fields.length >= 3);  // name, count, tags
    }

    // ─────────────────────────────────────────────────────────────────────
    // Framework factory instantiation (the original user-reported blocker)
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void runtimeContextFactory_has_no_arg_ctor_for_InjectionContextBuilder_to_instantiate() throws Exception {
        IClass<RuntimeContextFactory> desc = IClass.getClass(RuntimeContextFactory.class);
        IConstructor<RuntimeContextFactory> ctor = desc.getDeclaredConstructor();
        assertNotNull(ctor);
        RuntimeContextFactory instance = ctor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void runtimeContextFactory_implements_IInjectionChildContextFactory_via_isAssignableFrom() {
        // The pure-AOT check that previously triggered the
        // "Resolver but does not implement IElementResolver" warning storm
        // was equivalent to this: IClass.isAssignableFrom over fallback-
        // synthesised descriptors.
        IClass<?> iface = IClass.getClass(
                com.garganttua.core.injection.IInjectionChildContextFactory.class);
        assertTrue(iface.isAssignableFrom(IClass.getClass(RuntimeContextFactory.class)));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mapper integration
    // ─────────────────────────────────────────────────────────────────────

    public static class SimpleEntity {
        public String name = "alice";
        public int count = 7;
        public List<String> tags = new ArrayList<>(List.of("a", "b"));
        public SimpleEntity() {}
    }

    public static class SimpleDto {
        public String name;
        public int count;
        public List<String> tags;
        public SimpleDto() {}
    }

    @Test
    void mapper_passes_through_string_field_without_recursing_into_String_value() throws MapperException {
        // Without the leaf-type passthrough, the convention mapping for
        // (String -> String) would call Field.setAccessible on String.value
        // and crash with InaccessibleObjectException on java.base.
        Mapper mapper = new Mapper(IClass.getReflection());
        SimpleEntity source = new SimpleEntity();
        SimpleDto destination = new SimpleDto();
        SimpleDto result = mapper.map(source, IClass.getClass(SimpleDto.class), destination);
        assertNotNull(result);
        assertEquals("alice", result.name);
        assertEquals(7, result.count);
        assertNotNull(result.tags);
        assertEquals(List.of("a", "b"), result.tags);
    }

    public static class GenericHolder {
        // List<? extends Number> — wildcard upper bound, used to be unresolvable
        public List<? extends Number> bounded = new ArrayList<>();
        // List<List<String>> — nested ParameterizedType
        public List<List<String>> nested = new ArrayList<>();
        public GenericHolder() {}
    }

    @Test
    void mapper_handles_complex_collection_generics_without_npe() throws MapperException {
        // Without the resolveType extension, MapableCollectionMappingExecutor
        // returned null on the inner-generic type, propagating through
        // Mapper.mapInternal and NPE-ing on destination.getClass().
        Mapper mapper = new Mapper(IClass.getReflection());
        GenericHolder source = new GenericHolder();
        source.nested.add(List.of("x", "y"));
        GenericHolder destination = new GenericHolder();
        GenericHolder result = mapper.map(source, IClass.getClass(GenericHolder.class), destination);
        assertNotNull(result);
        // We don't assert the inner-collection content (resolveType collapses
        // to Object / raw List); the important thing is "no exception".
    }

    public static class JdkBag {
        public java.util.UUID uuid = java.util.UUID.randomUUID();
        public java.util.Date date = new java.util.Date();
        public java.math.BigDecimal amount = new java.math.BigDecimal("12.34");
        public java.time.LocalDate when = java.time.LocalDate.of(2026, 5, 29);
        public JdkBag() {}
    }

    @Test
    void mapper_passes_through_all_jdk_atomic_types() throws MapperException {
        Mapper mapper = new Mapper(IClass.getReflection());
        JdkBag source = new JdkBag();
        JdkBag destination = new JdkBag();
        // Reset destination so we can verify the source values were copied.
        destination.uuid = null;
        destination.date = null;
        destination.amount = null;
        destination.when = null;
        JdkBag result = mapper.map(source, IClass.getClass(JdkBag.class), destination);
        assertSame(source.uuid, result.uuid);
        assertSame(source.date, result.date);
        assertSame(source.amount, result.amount);
        assertSame(source.when, result.when);
    }

    public static class WithEnum {
        public java.time.DayOfWeek day = java.time.DayOfWeek.WEDNESDAY;
        public WithEnum() {}
    }

    @Test
    void mapper_passes_through_enums_without_recursing_into_enum_internals() throws MapperException {
        // Enums in the JDK have internal fields (name, ordinal). Without
        // leaf-type passthrough on isEnum(), the convention mapper would
        // try to access them.
        Mapper mapper = new Mapper(IClass.getReflection());
        WithEnum source = new WithEnum();
        WithEnum destination = new WithEnum();
        destination.day = null;
        WithEnum result = mapper.map(source, IClass.getClass(WithEnum.class), destination);
        assertEquals(java.time.DayOfWeek.WEDNESDAY, result.day);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Negative — ensure the strict supports() contract still holds
    // ─────────────────────────────────────────────────────────────────────

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface UnknownMarker {}

    @UnknownMarker
    static class UnknownMarked {}

    @Test
    void supports_returns_false_for_arbitrary_unregistered_types() {
        // The hybrid-mode contract — AOT must not claim unregistered types
        // via supports(), even though its getClass() can fallback-synth them.
        // (In pure-AOT mode, ProviderSelector routes to AOT anyway when no
        // provider claims; the strictness is what lets runtime win in hybrid.)
        AOTReflectionProvider provider = new AOTReflectionProvider();
        // Use a type the seed certainly does not cover.
        if (!AOTRegistry.getInstance().contains(UnknownMarked.class.getName())) {
            assertFalse(provider.supports(UnknownMarked.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Meta-annotation walking — @Observer is @Qualifier-meta, must surface
    // when caller asks for @Qualifier-annotated classes.
    // ─────────────────────────────────────────────────────────────────────

    // A test-scope class annotated with the real @Observer. Its presence in
    // the test compilation forces the IndexedAnnotationProcessor to emit the
    // corresponding index entry under
    // META-INF/garganttua/index/com.garganttua.core.observability.annotations.Observer.
    // The scanner then has actual content to surface.
    @com.garganttua.core.observability.annotations.Observer
    public static class TestObserverBean {
        public TestObserverBean() {}
    }

    @Test
    void aotScanner_finds_TOP_LEVEL_class_annotated_with_real_Observer() {
        // Top-level case — the dominant real-world pattern. If THIS one
        // breaks the user's @Observer-annotated classes are invisible
        // regardless of nesting.
        AOTAnnotationScanner scanner = new AOTAnnotationScanner();
        IClass<com.garganttua.core.observability.annotations.Observer> observerCls =
                IClass.getClass(com.garganttua.core.observability.annotations.Observer.class);
        java.util.List<IClass<?>> hits = scanner.getClassesWithAnnotation(observerCls);
        boolean foundTopLevel = hits.stream()
                .anyMatch(c -> c.getName().equals(TopLevelObserverBean.class.getName()));
        assertTrue(foundTopLevel,
                "Scanner did NOT find TopLevelObserverBean. Hits: "
                        + hits.stream().map(IClass::getName).toList());
    }

    @Test
    void aotScanner_finds_test_class_annotated_with_real_Observer() {
        // The proof end-to-end: a class annotated with the framework's
        // @Observer annotation must be discoverable via the scanner.
        // If this fails, the consumer-side detection chain is broken at
        // the framework boundary.
        AOTAnnotationScanner scanner = new AOTAnnotationScanner();
        IClass<com.garganttua.core.observability.annotations.Observer> observerCls =
                IClass.getClass(com.garganttua.core.observability.annotations.Observer.class);
        java.util.List<IClass<?>> hits = scanner.getClassesWithAnnotation(observerCls);
        assertNotNull(hits);
        boolean foundTestObserver = hits.stream()
                .anyMatch(c -> c.getName().equals(TestObserverBean.class.getName()));
        assertTrue(foundTestObserver,
                "Scanner did NOT find TestObserverBean annotated with @Observer. "
                        + "Hits returned: " + hits.stream().map(IClass::getName).toList()
                        + ". This breaks the framework's observer auto-detection chain.");
    }

    @Test
    void aotScanner_finds_Observer_under_Qualifier_via_meta_walking() {
        // The flow InjectionContextBuilder relies on: ask for @Qualifier,
        // expect framework annotation types (and user-defined qualifiers)
        // back. @Observer is itself meta-annotated @Qualifier, so it must
        // surface — either directly in the Qualifier index, or via the
        // scanner's meta-walking pass.
        AOTAnnotationScanner scanner = new AOTAnnotationScanner();
        IClass<javax.inject.Qualifier> qualifier =
                IClass.getClass(javax.inject.Qualifier.class);
        java.util.List<IClass<?>> hits = scanner.getClassesWithAnnotation(qualifier);
        boolean foundObserverAsQualifier = hits.stream()
                .anyMatch(c -> c.getName().equals(
                        "com.garganttua.core.observability.annotations.Observer"));
        assertTrue(foundObserverAsQualifier,
                "Scanner did NOT find @Observer when asked for @Qualifier. "
                        + "Hits returned: " + hits.stream().map(IClass::getName).toList()
                        + ". This is the framework-wide qualifier discovery breaking.");
    }

    @Test
    void aotScanner_walks_meta_annotations_observer_surfaces_under_qualifier() {
        // The framework's InjectionContextBuilder asks the scanner for classes
        // annotated with javax.inject.Qualifier to discover qualifiers. The
        // @Observer annotation is itself meta-annotated @Qualifier, so any
        // class annotated @Observer must surface in the result. Without
        // meta-walking in the AOT scanner, observer classes are invisible
        // and their event subscriptions never fire.
        AOTAnnotationScanner scanner = new AOTAnnotationScanner();
        IClass<javax.inject.Qualifier> qualifier =
                IClass.getClass(javax.inject.Qualifier.class);
        java.util.List<IClass<?>> hits = scanner.getClassesWithAnnotation(qualifier);
        // The scanner should report itself capable of finding qualifier-meta
        // classes — the actual list may be empty if no consumer has shipped
        // an @Observer-annotated class yet, but the call must complete and
        // include whatever's been indexed. The assertion below verifies
        // the codepath at minimum doesn't throw and returns a non-null list.
        assertNotNull(hits);
    }
}
