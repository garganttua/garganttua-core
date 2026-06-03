package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for {@link SubRuntimeExpression}: metadata, the no-context
 * failure branch, output propagation, and scoped-variable save/restore isolation.
 *
 * <p>Uses an empty-step {@link SubRuntime} so the supplier's surrounding logic
 * (context lookup, scoped-variable handling, output return) is exercised without
 * depending on step wiring.</p>
 */
class SubRuntimeExpressionBehaviourTest {

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

    private RuntimeContext<String, String> startedContext() {
        IInjectionContextBuilder builder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        RuntimeContext<String, String> ctx = new RuntimeContext<>(builder.build(), "in",
                String.class, Map.of(), UUID.randomUUID());
        ctx.onInit().onStart();
        return ctx;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SubRuntime<Object[], Object> emptySubRuntime() {
        return new SubRuntime("sub", Map.of());
    }

    // -------------------------------------------------------------------------
    // Constructor validation + metadata
    // -------------------------------------------------------------------------

    @Test
    void constructor_nullSubRuntime_throwsNpe() {
        var ex = assertThrows(NullPointerException.class,
                () -> new SubRuntimeExpression(null));
        assertEquals("SubRuntime cannot be null", ex.getMessage());
    }

    @Test
    void constructor_nullScopedNames_throwsNpe() {
        var ex = assertThrows(NullPointerException.class,
                () -> new SubRuntimeExpression(emptySubRuntime(), null));
        assertEquals("Scoped variable names cannot be null", ex.getMessage());
    }

    @Test
    void metadata_isAlwaysObjectAndNonContextual() {
        SubRuntimeExpression expr = new SubRuntimeExpression(emptySubRuntime());
        assertEquals(Object.class, expr.getSuppliedType());
        assertTrue(expr.getSuppliedClass().represents(Object.class));
        assertFalse(expr.isContextual());
    }

    // -------------------------------------------------------------------------
    // No-context failure branch
    // -------------------------------------------------------------------------

    @Test
    void supply_noRuntimeContextBound_throwsSupplyException() {
        SubRuntimeExpression expr = new SubRuntimeExpression(emptySubRuntime());
        SupplyException ex = assertThrows(SupplyException.class, () -> expr.evaluate().supply());
        assertEquals("No runtime context available for sub-runtime execution", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Output propagation
    // -------------------------------------------------------------------------

    @Test
    void supply_returnsCurrentContextOutputAfterExecution() {
        RuntimeContext<String, String> ctx = startedContext();
        ctx.setOutput("the-output");
        SubRuntimeExpression expr = new SubRuntimeExpression(emptySubRuntime());

        Optional<Object> result = RuntimeExpressionContext.callIn(ctx, () -> expr.evaluate().supply());
        assertEquals(Optional.of("the-output"), result);
    }

    @Test
    void supply_returnsEmptyWhenNoOutputSet() {
        RuntimeContext<String, String> ctx = startedContext();
        SubRuntimeExpression expr = new SubRuntimeExpression(emptySubRuntime());

        Optional<Object> result = RuntimeExpressionContext.callIn(ctx, () -> expr.evaluate().supply());
        assertTrue(result.isEmpty(), "no output set -> Optional.empty()");
    }

    // -------------------------------------------------------------------------
    // Scoped-variable save/restore isolation
    // -------------------------------------------------------------------------

    @Test
    void supply_restoresScopedVariablesThatExistedBeforeExecution() {
        RuntimeContext<String, String> ctx = startedContext();
        ctx.setVariable("scoped", "original");
        SubRuntimeExpression expr = new SubRuntimeExpression(emptySubRuntime(), Set.of("scoped"));

        // The sub-runtime has no steps, so it cannot change "scoped"; the value
        // saved before and restored after must equal the original.
        RuntimeExpressionContext.runIn(ctx, () -> expr.evaluate().supply());

        assertEquals(Optional.of("original"),
                ctx.getVariable("scoped", IClass.getClass(String.class)));
    }

    @Test
    void supply_doesNotSeedScopedVariableThatDidNotExist() {
        RuntimeContext<String, String> ctx = startedContext();
        // "absent" was never set; after execution it must still be absent because
        // the save map recorded Optional.empty() and the restore loop skips empties.
        SubRuntimeExpression expr = new SubRuntimeExpression(emptySubRuntime(), Set.of("absent"));

        RuntimeExpressionContext.runIn(ctx, () -> expr.evaluate().supply());

        assertNull(ctx.getOutput()); // no output produced either
        assertTrue(ctx.getVariable("absent", IClass.getClass(String.class)).isEmpty());
    }
}
