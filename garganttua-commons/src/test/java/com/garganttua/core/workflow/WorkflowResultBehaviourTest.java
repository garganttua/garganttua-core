package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class WorkflowResultBehaviourTest {

    private final UUID uuid = UUID.randomUUID();
    private final Instant start = Instant.ofEpochMilli(1_000);
    private final Instant stop = Instant.ofEpochMilli(1_500);

    // --- compact constructor normalization ---

    @Test
    void nullMapsBecomeEmptyUnmodifiable() {
        WorkflowResult r = new WorkflowResult(uuid, null, 0, null, null, start, stop, null, null);
        assertTrue(r.variables().isEmpty());
        assertTrue(r.stageOutputs().isEmpty());
        assertTrue(r.exception().isEmpty());
        assertTrue(r.exceptionMessage().isEmpty());
    }

    @Test
    void providedMapsAreUnmodifiable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        WorkflowResult r = new WorkflowResult(uuid, null, 0, vars, null, start, stop, Optional.empty(),
                Optional.empty());
        assertThrows(UnsupportedOperationException.class, () -> r.variables().put("b", 2));
    }

    // --- isSuccess / hasAborted ---

    @Test
    void isSuccessRequiresCodeZeroAndNoException() {
        WorkflowResult ok = WorkflowResult.success(uuid, "out", 0, Map.of(), Map.of(), start, stop);
        assertTrue(ok.isSuccess());
        assertFalse(ok.hasAborted());
    }

    @Test
    void nonZeroCodeIsNotSuccess() {
        WorkflowResult r = WorkflowResult.success(uuid, "out", 5, Map.of(), Map.of(), start, stop);
        assertFalse(r.isSuccess());
    }

    @Test
    void nullCodeIsNotSuccess() {
        WorkflowResult r = new WorkflowResult(uuid, null, null, Map.of(), Map.of(), start, stop,
                Optional.empty(), Optional.empty());
        assertFalse(r.isSuccess());
    }

    @Test
    void presentExceptionMeansAbortedAndNotSuccess() {
        WorkflowResult r = WorkflowResult.failure(uuid, start, stop, new RuntimeException("boom"));
        assertTrue(r.hasAborted());
        assertFalse(r.isSuccess());
        assertEquals(-1, r.code());
        assertEquals("boom", r.exceptionMessage().orElse(null));
    }

    @Test
    void failureWithNullMessageYieldsEmptyMessageOptional() {
        WorkflowResult r = WorkflowResult.failure(uuid, start, stop, new RuntimeException());
        assertTrue(r.hasAborted());
        assertTrue(r.exceptionMessage().isEmpty());
    }

    // --- duration ---

    @Test
    void durationIsStopMinusStart() {
        WorkflowResult r = WorkflowResult.success(uuid, null, 0, Map.of(), Map.of(), start, stop);
        assertEquals(Duration.ofMillis(500), r.duration());
    }

    // --- getStageOutput ---

    @Test
    void getStageOutputReturnsTypedValue() {
        Map<String, Object> stageOutputs = Map.of("validation.amount", 42);
        WorkflowResult r = new WorkflowResult(uuid, null, 0, Map.of(), stageOutputs, start, stop,
                Optional.empty(), Optional.empty());
        Optional<Integer> v = r.getStageOutput("validation", "amount", JdkClass.of(Integer.class));
        assertTrue(v.isPresent());
        assertEquals(42, v.get());
    }

    @Test
    void getStageOutputEmptyWhenMissing() {
        WorkflowResult r = WorkflowResult.success(uuid, null, 0, Map.of(), Map.of(), start, stop);
        assertTrue(r.getStageOutput("validation", "amount", JdkClass.of(Integer.class)).isEmpty());
    }

    @Test
    void getStageOutputEmptyWhenTypeMismatch() {
        Map<String, Object> stageOutputs = Map.of("validation.amount", "notAnInt");
        WorkflowResult r = new WorkflowResult(uuid, null, 0, Map.of(), stageOutputs, start, stop,
                Optional.empty(), Optional.empty());
        Optional<Integer> v = r.getStageOutput("validation", "amount", JdkClass.of(Integer.class));
        assertTrue(v.isEmpty());
    }

    // --- getVariable ---

    @Test
    void getVariableReturnsTypedValue() {
        WorkflowResult r = new WorkflowResult(uuid, null, 0, Map.of("user", "alice"), Map.of(), start, stop,
                Optional.empty(), Optional.empty());
        Optional<String> v = r.getVariable("user", JdkClass.of(String.class));
        assertEquals("alice", v.orElse(null));
    }

    @Test
    void getVariableEmptyWhenTypeMismatch() {
        WorkflowResult r = new WorkflowResult(uuid, null, 0, Map.of("count", 3), Map.of(), start, stop,
                Optional.empty(), Optional.empty());
        IClass<String> stringClass = JdkClass.of(String.class);
        assertTrue(r.getVariable("count", stringClass).isEmpty());
    }

    @Test
    void getVariableEmptyWhenMissing() {
        WorkflowResult r = WorkflowResult.success(uuid, null, 0, Map.of(), Map.of(), start, stop);
        assertTrue(r.getVariable("nope", JdkClass.of(String.class)).isEmpty());
    }

    // --- success factory wiring ---

    @Test
    void successFactoryPreservesFields() {
        Object out = new Object();
        WorkflowResult r = WorkflowResult.success(uuid, out, 0, Map.of("v", 1), Map.of("s.o", 2), start, stop);
        assertSame(out, r.output());
        assertSame(uuid, r.uuid());
        assertEquals(0, r.code());
        assertTrue(r.exception().isEmpty());
    }
}
