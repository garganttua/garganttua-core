package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;

/**
 * Behaviour tests for {@link Runtime}: constructor validation, observer
 * registration, and execution semantics (result, code, observability events,
 * per-execution uuid).
 */
class RuntimeBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    private IInjectionContext injectionContext() {
        IInjectionContextBuilder ctx = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        ctx.build().onInit().onStart();
        return (IInjectionContext) ctx.build();
    }

    @SuppressWarnings("unchecked")
    private Runtime<String, String> autodetectedRuntime() {
        IInjectionContextBuilder ctx = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        ctx.build().onInit().onStart();
        IRuntimesBuilder b = RuntimesBuilder.builder().provide(reflectionBuilder).provide(ctx).autoDetect(true);
        Map<String, IRuntime<?, ?>> runtimes = b.build();
        return (Runtime<String, String>) runtimes.get("runtime-1");
    }

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Test
    void constructor_nullSteps_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new Runtime<>(
                "rt", null, injectionContext(), String.class, String.class, Map.of()));
        assertEquals("Steps map cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullInputType_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new Runtime<String, String>(
                "rt", new LinkedHashMap<>(), injectionContext(), null, String.class, Map.of()));
        assertEquals("Input type cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullOutputType_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new Runtime<String, String>(
                "rt", new LinkedHashMap<>(), injectionContext(), String.class, null, Map.of()));
        assertEquals("Output Type cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullName_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new Runtime<>(
                null, new LinkedHashMap<>(), injectionContext(), String.class, String.class, Map.of()));
        assertEquals("Name cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullInjectionContext_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new Runtime<>(
                "rt", new LinkedHashMap<>(), null, String.class, String.class, Map.of()));
        assertEquals("Context cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullVariables_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new Runtime<String, String>(
                "rt", new LinkedHashMap<>(), injectionContext(), String.class, String.class, null));
        assertEquals("Preset variables map cannot be null", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    @Test
    void execute_returnsExpectedOutputAndSuccessCode() {
        Runtime<String, String> runtime = autodetectedRuntime();
        IRuntimeResult<String, String> result = runtime.execute("input").orElseThrow();

        assertEquals("input-processed-fixed-value-in-method-preset-variable", result.output());
        assertEquals(201, result.code());
        assertFalse(result.hasAborted());
    }

    @Test
    void execute_eachCallUsesDistinctUuid() {
        Runtime<String, String> runtime = autodetectedRuntime();
        Object uuid1 = runtime.execute("input").orElseThrow().uuid();
        Object uuid2 = runtime.execute("input").orElseThrow().uuid();
        assertNotEquals(uuid1, uuid2);
    }

    @Test
    void execute_withExplicitUuid_propagatesThatUuidIntoResult() {
        Runtime<String, String> runtime = autodetectedRuntime();
        UUID uuid = UUID.randomUUID();
        IRuntimeResult<String, String> result = runtime.execute(uuid, "input").orElseThrow();
        assertEquals(uuid, result.uuid());
    }

    // -------------------------------------------------------------------------
    // Observability
    // -------------------------------------------------------------------------

    @Test
    void execute_firesStartAndEndEventsUnderRuntimeSource() {
        Runtime<String, String> runtime = autodetectedRuntime();
        List<ObservableEvent> events = new CopyOnWriteArrayList<>();
        IObserver<ObservableEvent> observer = events::add;
        runtime.addObserver(observer);

        runtime.execute("input").orElseThrow();

        boolean startFired = events.stream()
                .anyMatch(e -> e instanceof StartEvent && "runtime:runtime-1".equals(e.source()));
        boolean endFired = events.stream()
                .anyMatch(e -> e instanceof EndEvent && "runtime:runtime-1".equals(e.source()));
        assertTrue(startFired, "a StartEvent for runtime:runtime-1 must be fired");
        assertTrue(endFired, "an EndEvent for runtime:runtime-1 must be fired");
    }

    @Test
    void removeObserver_stopsReceivingEvents() {
        Runtime<String, String> runtime = autodetectedRuntime();
        List<ObservableEvent> events = new CopyOnWriteArrayList<>();
        IObserver<ObservableEvent> observer = events::add;

        runtime.addObserver(observer);
        runtime.removeObserver(observer);
        runtime.execute("input").orElseThrow();

        assertTrue(events.isEmpty(), "removed observer must not receive any event");
    }
}
