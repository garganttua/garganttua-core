package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

/**
 * Behaviour tests for {@link RuntimeContext}: constructor validation, lifecycle
 * guards on accessors, variable/output/code handling, exception recording and
 * the static contextual-supplier factory methods.
 */
class RuntimeContextBehaviourTest {

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

    private com.garganttua.core.injection.IInjectionContext injectionContext() {
        IInjectionContextBuilder ctx = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        return ctx.build();
    }

    private RuntimeContext<String, String> newStartedContext(String input,
            Map<String, ISupplier<?>> presets) {
        RuntimeContext<String, String> context = new RuntimeContext<>(injectionContext(), input,
                String.class, presets, UUID.randomUUID());
        context.onInit().onStart();
        return context;
    }

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Test
    void constructor_nullUuid_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class,
                () -> new RuntimeContext<>(injectionContext(), "in", String.class, Map.of(), null));
        assertEquals("Uuid cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullParent_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class,
                () -> new RuntimeContext<>(null, "in", String.class, Map.of(), UUID.randomUUID()));
        assertEquals("Parent context cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullInput_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class,
                () -> new RuntimeContext<>(injectionContext(), null, String.class, Map.of(), UUID.randomUUID()));
        assertEquals("Input type cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullOutputType_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class,
                () -> new RuntimeContext<String, String>(injectionContext(), "in", null, Map.of(), UUID.randomUUID()));
        assertEquals("Output type cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullPresetVariables_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class,
                () -> new RuntimeContext<>(injectionContext(), "in", String.class, null, UUID.randomUUID()));
        assertEquals("Preset variables map cannot be null", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Lifecycle guards
    // -------------------------------------------------------------------------

    @Test
    void getInput_beforeStart_throwsRuntimeException() {
        RuntimeContext<String, String> context = new RuntimeContext<>(injectionContext(), "in",
                String.class, Map.of(), UUID.randomUUID());
        // Accessors are guarded by ensureInitializedAndStarted(); a LifecycleException
        // is wrapped into a RuntimeException by wrapLifecycle.
        assertThrows(RuntimeException.class, context::getInput);
    }

    @Test
    void getInput_afterStart_returnsInputWrappedInOptional() {
        RuntimeContext<String, String> context = newStartedContext("hello", Map.of());
        assertEquals(Optional.of("hello"), context.getInput());
    }

    // -------------------------------------------------------------------------
    // Code handling
    // -------------------------------------------------------------------------

    @Test
    void getCode_defaultsToGenericSuccessCode() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        assertEquals(Optional.of(IRuntime.GENERIC_RUNTIME_SUCCESS_CODE), context.getCode());
    }

    @Test
    void setCode_thenGetCode_returnsTheSetValue() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        context.setCode(418);
        assertEquals(Optional.of(418), context.getCode());
    }

    // -------------------------------------------------------------------------
    // Output handling
    // -------------------------------------------------------------------------

    @Test
    void setOutput_thenGetOutput_returnsValue() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        context.setOutput("the-output");
        assertEquals("the-output", context.getOutput());
    }

    @Test
    void getOutputType_returnsDeclaredOutputType() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        assertTrue(context.getOutputType().represents(String.class));
    }

    @Test
    void isOfOutputType_matchingType_returnsTrue() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        assertTrue(context.isOfOutputType(IClass.getClass(String.class)));
    }

    @Test
    void isOfOutputType_incompatibleType_returnsFalse() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        // String is not assignable from Integer, so the strict check returns false.
        assertFalse(context.isOfOutputType(IClass.getClass(Integer.class)));
    }

    // -------------------------------------------------------------------------
    // Variables
    // -------------------------------------------------------------------------

    @Test
    void setVariable_thenGetVariable_returnsStoredValue() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        context.setVariable("greeting", "bonjour");
        assertEquals(Optional.of("bonjour"), context.getVariable("greeting", IClass.getClass(String.class)));
    }

    @Test
    void getVariable_unknownName_returnsEmpty() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        assertEquals(Optional.empty(), context.getVariable("does-not-exist", IClass.getClass(String.class)));
    }

    @Test
    void presetVariables_areSeededOnStart() {
        ISupplier<String> preset = FixedSupplierBuilder.of("preset-value").build();
        RuntimeContext<String, String> context = newStartedContext("x",
                Map.of("seeded", preset));
        assertEquals(Optional.of("preset-value"),
                context.getVariable("seeded", IClass.getClass(String.class)));
    }

    // -------------------------------------------------------------------------
    // Exception recording
    // -------------------------------------------------------------------------

    @Test
    void recordException_thenFindException_byWildcardPattern_returnsRecord() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        RuntimeExceptionRecord record = new RuntimeExceptionRecord("rt", "step",
                IClass.getClass(IllegalStateException.class), new IllegalStateException("boom"),
                500, false, "ref");
        context.recordException(record);

        RuntimeExceptionRecord wildcard = new RuntimeExceptionRecord(null, "step", null, null, null, null, null);
        Optional<RuntimeExceptionRecord> found = context.findException(wildcard);
        assertTrue(found.isPresent());
        assertSame(record, found.get());
    }

    @Test
    void findAbortingExceptionReport_returnsOnlyAbortingOne() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        context.recordException(new RuntimeExceptionRecord("rt", "s1",
                IClass.getClass(RuntimeException.class), new RuntimeException("non-abort"), 1, false, "r1"));
        RuntimeExceptionRecord aborting = new RuntimeExceptionRecord("rt", "s2",
                IClass.getClass(RuntimeException.class), new RuntimeException("aborting"), 2, true, "r2");
        context.recordException(aborting);

        Optional<RuntimeExceptionRecord> found = context.findAbortingExceptionReport();
        assertTrue(found.isPresent());
        assertSame(aborting, found.get());
    }

    @Test
    void getExceptionMessage_returnsAbortingExceptionMessage() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        context.recordException(new RuntimeExceptionRecord("rt", "s",
                IClass.getClass(RuntimeException.class), new RuntimeException("explosion"), 9, true, "r"));
        assertEquals(Optional.of("explosion"), context.getExceptionMessage());
    }

    @Test
    void getExceptionMessage_withNoAbortingException_returnsEmpty() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        assertEquals(Optional.empty(), context.getExceptionMessage());
    }

    @Test
    void getException_matchingType_returnsTheRecordedExceptionInstance() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        IllegalArgumentException thrown = new IllegalArgumentException("bad arg");
        context.recordException(new RuntimeExceptionRecord("rt", "s",
                IClass.getClass(IllegalArgumentException.class), thrown, 7, true, "r"));

        Optional<IllegalArgumentException> found = context.getException(IClass.getClass(IllegalArgumentException.class));
        assertTrue(found.isPresent());
        assertSame(thrown, found.get());
    }

    @Test
    void getException_nonMatchingType_returnsEmpty() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        context.recordException(new RuntimeExceptionRecord("rt", "s",
                IClass.getClass(IllegalArgumentException.class), new IllegalArgumentException("x"), 7, true, "r"));

        // No aborting NumberFormatException recorded.
        assertEquals(Optional.empty(),
                context.getException(IClass.getClass(NumberFormatException.class)));
    }

    // -------------------------------------------------------------------------
    // uuid / result
    // -------------------------------------------------------------------------

    @Test
    void uuid_returnsTheConstructorUuid() {
        UUID uuid = UUID.randomUUID();
        RuntimeContext<String, String> context = new RuntimeContext<>(injectionContext(), "x",
                String.class, Map.of(), uuid);
        assertEquals(uuid, context.uuid());
    }

    @Test
    void getResult_afterStopAndCode_carriesUuidCodeAndVariables() {
        UUID uuid = UUID.randomUUID();
        RuntimeContext<String, String> context = new RuntimeContext<>(injectionContext(), "in",
                String.class, Map.of(), uuid);
        context.onInit().onStart();
        context.setVariable("k", "v");
        context.setCode(201);
        context.setOutput("done");
        context.onStop();

        IRuntimeResult<String, String> result = context.getResult();
        assertEquals(uuid, result.uuid());
        assertEquals(201, result.code());
        assertEquals("done", result.output());
        assertEquals("v", result.variables().get("k"));
        assertFalse(result.hasAborted());
    }

    @Test
    void copy_isDeprecated_butReturnsSelfWhenStarted() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        assertDoesNotThrow(() -> assertSame(context, context.copy()));
    }

    // -------------------------------------------------------------------------
    // Static contextual supplier factories
    // -------------------------------------------------------------------------

    @Test
    void staticVariableSupplier_resolvesVariableFromContext() {
        RuntimeContext<String, String> context = newStartedContext("x",
                Map.of("greeting", FixedSupplierBuilder.of("hi").build()));

        var supplier = RuntimeContext.<String, String, String>variable("greeting", IClass.getClass(String.class))
                .build();
        @SuppressWarnings("unchecked")
        Optional<String> value = supplier.supply(context);
        assertEquals(Optional.of("hi"), value);
    }

    @Test
    void staticInputSupplier_resolvesInputFromContext() {
        RuntimeContext<String, String> context = newStartedContext("the-input", Map.of());

        var supplier = RuntimeContext.<String, String>input(IClass.getClass(String.class)).build();
        @SuppressWarnings("unchecked")
        Optional<String> value = supplier.supply(context);
        assertEquals(Optional.of("the-input"), value);
    }

    @Test
    void staticCodeSupplier_resolvesCurrentCode() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());
        context.setCode(404);

        var supplier = RuntimeContext.<String, String>code().build();
        @SuppressWarnings("unchecked")
        Optional<Integer> value = supplier.supply(context);
        assertEquals(Optional.of(404), value);
    }

    @Test
    void staticContextSupplier_yieldsTheContextItself() {
        RuntimeContext<String, String> context = newStartedContext("x", Map.of());

        var supplier = RuntimeContext.<String, String>context().build();
        @SuppressWarnings("unchecked")
        Optional<?> value = supplier.supply(context);
        assertTrue(value.isPresent());
        assertSame(context, value.get());
    }
}
