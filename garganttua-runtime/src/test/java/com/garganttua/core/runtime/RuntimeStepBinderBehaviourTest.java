package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for {@link RuntimeStepMethodBinder} and
 * {@link RuntimeStepFallbackBinder}: constructor validation and accessor
 * contracts. A minimal real {@link IExpression} stub backed by a
 * {@link FixedSupplier} is used so no expression DSL is required.
 */
class RuntimeStepBinderBehaviourTest {

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

    /** Minimal real expression yielding a fixed String value. */
    private static final class StubExpression
            implements IExpression<String, ISupplier<String>> {
        private final String value;

        StubExpression(String value) {
            this.value = value;
        }

        @Override
        public ISupplier<String> evaluate() {
            return new FixedSupplier<>(value, IClass.getClass(String.class));
        }

        @Override
        public IClass<String> getSuppliedClass() {
            return IClass.getClass(String.class);
        }

        @Override
        public Type getSuppliedType() {
            return String.class;
        }

        @Override
        public boolean isContextual() {
            return false;
        }
    }

    private RuntimeStepMethodBinder<String, String, String> methodBinder(
            Optional<String> variable, boolean isOutput, boolean nullable) {
        return new RuntimeStepMethodBinder<>("rt", "step", new StubExpression("v"),
                variable, isOutput, 201, Set.of(), Optional.empty(), false, nullable, "expr-ref");
    }

    private RuntimeStepFallbackBinder<String, String, String> fallbackBinder(
            Optional<String> variable, boolean isOutput, boolean nullable) {
        return new RuntimeStepFallbackBinder<>("rt", "step", new StubExpression("fb"),
                variable, isOutput, List.of(), nullable, "fb-ref");
    }

    // -------------------------------------------------------------------------
    // MethodBinder — constructor validation
    // -------------------------------------------------------------------------

    @Test
    void methodBinder_nullRuntimeName_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<>(
                null, "step", new StubExpression("v"), Optional.empty(), false, 0,
                Set.of(), Optional.empty(), false, false, "ref"));
        assertEquals("runtimeName cannot be null", ex.getMessage());
    }

    @Test
    void methodBinder_nullExpression_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<String, String, String>(
                "rt", "step", null, Optional.empty(), false, 0,
                Set.of(), Optional.empty(), false, false, "ref"));
        assertEquals("Expression cannot be null", ex.getMessage());
    }

    @Test
    void methodBinder_nullSuccessCode_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, null,
                Set.of(), Optional.empty(), false, false, "ref"));
        assertEquals("Success code cannot be null", ex.getMessage());
    }

    @Test
    void methodBinder_nullCatches_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, 0,
                null, Optional.empty(), false, false, "ref"));
        assertEquals("Catches cannot be null", ex.getMessage());
    }

    @Test
    void methodBinder_nullAbortFlag_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, 0,
                Set.of(), Optional.empty(), null, false, "ref"));
        assertEquals("abortOnUncatchedException cannot be null", ex.getMessage());
    }

    @Test
    void methodBinder_nullNullableFlag_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, 0,
                Set.of(), Optional.empty(), false, null, "ref"));
        assertEquals("nullable cannot be null", ex.getMessage());
    }

    @Test
    void methodBinder_nullCondition_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, 0,
                Set.of(), (Optional<ICondition>) null, false, false, "ref"));
        assertEquals("Condition optional cannot be null", ex.getMessage());
    }

    @Test
    void methodBinder_nullPipes_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepMethodBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, 0,
                Set.of(), null, Optional.empty(), false, false, "ref"));
        assertEquals("Pipes cannot be null", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // MethodBinder — accessors
    // -------------------------------------------------------------------------

    @Test
    void methodBinder_accessorsReflectConstructorArguments() {
        RuntimeStepMethodBinder<String, String, String> binder =
                methodBinder(Optional.of("myVar"), true, true);

        assertTrue(binder.isOutput());
        assertTrue(binder.nullable());
        assertEquals(Optional.of("myVar"), binder.variable());
        assertEquals("expr-ref", binder.getExecutableReference());
        assertEquals(String.class, binder.getSuppliedType());
        assertTrue(binder.getSuppliedClass().represents(IMethodReturn.class));
        assertEquals(Set.of(), binder.dependencies());
        assertEquals(0, binder.getParametersContextTypes().length);
    }

    @Test
    void methodBinder_falseFlagsAndAbsentVariable_areReflected() {
        RuntimeStepMethodBinder<String, String, String> binder =
                methodBinder(Optional.empty(), false, false);

        assertFalse(binder.isOutput());
        assertFalse(binder.nullable());
        assertEquals(Optional.empty(), binder.variable());
    }

    // -------------------------------------------------------------------------
    // FallbackBinder — constructor validation
    // -------------------------------------------------------------------------

    @Test
    void fallbackBinder_nullStepName_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepFallbackBinder<>(
                "rt", null, new StubExpression("v"), Optional.empty(), false, List.of(), false, "ref"));
        assertEquals("stepName cannot be null", ex.getMessage());
    }

    @Test
    void fallbackBinder_nullExpression_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepFallbackBinder<String, String, String>(
                "rt", "step", null, Optional.empty(), false, List.of(), false, "ref"));
        assertEquals("Expression cannot be null", ex.getMessage());
    }

    @Test
    void fallbackBinder_nullOnExceptions_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepFallbackBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, null, false, "ref"));
        assertEquals("OnException list cannot be null", ex.getMessage());
    }

    @Test
    void fallbackBinder_nullIsOutput_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepFallbackBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), null, List.of(), false, "ref"));
        assertEquals("Is output cannot be null", ex.getMessage());
    }

    @Test
    void fallbackBinder_nullNullable_throwsNpeWithMessage() {
        var ex = assertThrows(NullPointerException.class, () -> new RuntimeStepFallbackBinder<>(
                "rt", "step", new StubExpression("v"), Optional.empty(), false, List.of(), null, "ref"));
        assertEquals("Nullable cannot be null", ex.getMessage());
    }

    @Test
    void fallbackBinder_nullVariableOptional_throwsNpe() {
        // NOTE: the constructor logs variable.isPresent() before the explicit
        // Objects.requireNonNull(variable, "Variable optional cannot be null"),
        // so a null variable actually fails on the log call. We assert the type
        // (NPE) without binding to the unreachable validation message.
        assertThrows(NullPointerException.class, () -> new RuntimeStepFallbackBinder<>(
                "rt", "step", new StubExpression("v"), null, false, List.of(), false, "ref"));
    }

    // -------------------------------------------------------------------------
    // FallbackBinder — accessors
    // -------------------------------------------------------------------------

    @Test
    void fallbackBinder_accessorsReflectConstructorArguments() {
        RuntimeStepFallbackBinder<String, String, String> binder =
                fallbackBinder(Optional.of("fbVar"), true, true);

        assertTrue(binder.isOutput());
        assertTrue(binder.nullable());
        assertEquals(Optional.of("fbVar"), binder.variable());
        assertEquals("fb-ref", binder.getExecutableReference());
        assertEquals(String.class, binder.getSuppliedType());
        assertTrue(binder.getSuppliedClass().represents(IMethodReturn.class));
        assertEquals(Set.of(), binder.dependencies());
        assertEquals(0, binder.getParametersContextTypes().length);
    }

    @Test
    void fallbackBinder_falseFlagsAndAbsentVariable_areReflected() {
        RuntimeStepFallbackBinder<String, String, String> binder =
                fallbackBinder(Optional.empty(), false, false);

        assertFalse(binder.isOutput());
        assertFalse(binder.nullable());
        assertEquals(Optional.empty(), binder.variable());
    }
}
