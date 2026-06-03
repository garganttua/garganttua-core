package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class RuntimeExceptionRecordBehaviourTest {

    @SuppressWarnings("unchecked")
    private static IClass<? extends Throwable> type(Class<? extends Throwable> c) {
        return (IClass<? extends Throwable>) (IClass<?>) JdkClass.of(c);
    }

    private RuntimeExceptionRecord record() {
        return new RuntimeExceptionRecord("orderProcessing", "validateAmount",
                type(IllegalArgumentException.class),
                new IllegalArgumentException("Amount must be positive"),
                400, true, "OrderValidator.validate()");
    }

    // --- exceptionMessage ---

    @Test
    void exceptionMessageReturnsUnderlyingMessage() {
        assertEquals("Amount must be positive", record().exceptionMessage());
    }

    @Test
    void exceptionMessageThrowsWhenNoException() {
        RuntimeExceptionRecord r = new RuntimeExceptionRecord(null, null, null, null, null, null, null);
        assertThrows(NullPointerException.class, r::exceptionMessage);
    }

    // --- matches(pattern) ---

    @Test
    void nullPatternNeverMatches() {
        assertFalse(record().matches((RuntimeExceptionRecord) null));
    }

    @Test
    void fullyNullPatternMatchesAnything() {
        RuntimeExceptionRecord pattern = new RuntimeExceptionRecord(null, null, null, null, null, null, null);
        assertTrue(record().matches(pattern));
    }

    @Test
    void runtimeNameMustMatchExactly() {
        RuntimeExceptionRecord ok = new RuntimeExceptionRecord("orderProcessing", null, null, null, null, null, null);
        RuntimeExceptionRecord ko = new RuntimeExceptionRecord("other", null, null, null, null, null, null);
        assertTrue(record().matches(ok));
        assertFalse(record().matches(ko));
    }

    @Test
    void stepNameMustMatchExactly() {
        RuntimeExceptionRecord ok = new RuntimeExceptionRecord(null, "validateAmount", null, null, null, null, null);
        RuntimeExceptionRecord ko = new RuntimeExceptionRecord(null, "otherStep", null, null, null, null, null);
        assertTrue(record().matches(ok));
        assertFalse(record().matches(ko));
    }

    @Test
    void exceptionTypeMatchesPolymorphically() {
        // pattern RuntimeException is assignable-from IllegalArgumentException -> match
        RuntimeExceptionRecord superPattern = new RuntimeExceptionRecord(null, null,
                type(java.lang.RuntimeException.class), null, null, null, null);
        assertTrue(record().matches(superPattern));

        // exact type matches
        RuntimeExceptionRecord exactPattern = new RuntimeExceptionRecord(null, null,
                type(IllegalArgumentException.class), null, null, null, null);
        assertTrue(record().matches(exactPattern));
    }

    @Test
    void exceptionTypeMoreSpecificDoesNotMatch() {
        // pattern NumberFormatException is NOT assignable-from IllegalArgumentException
        RuntimeExceptionRecord subPattern = new RuntimeExceptionRecord(null, null,
                type(NumberFormatException.class), null, null, null, null);
        assertFalse(record().matches(subPattern));
    }

    @Test
    void exceptionTypePatternAgainstNullRecordTypeDoesNotMatch() {
        RuntimeExceptionRecord noType = new RuntimeExceptionRecord("r", "s", null,
                new java.lang.RuntimeException(), null, null, null);
        RuntimeExceptionRecord pattern = new RuntimeExceptionRecord(null, null,
                type(RuntimeException.class), null, null, null, null);
        assertFalse(noType.matches(pattern));
    }

    @Test
    void codeMustMatchExactly() {
        RuntimeExceptionRecord ok = new RuntimeExceptionRecord(null, null, null, null, 400, null, null);
        RuntimeExceptionRecord ko = new RuntimeExceptionRecord(null, null, null, null, 500, null, null);
        assertTrue(record().matches(ok));
        assertFalse(record().matches(ko));
    }

    @Test
    void hasAbortedMustMatchExactly() {
        RuntimeExceptionRecord ok = new RuntimeExceptionRecord(null, null, null, null, null, true, null);
        RuntimeExceptionRecord ko = new RuntimeExceptionRecord(null, null, null, null, null, false, null);
        assertTrue(record().matches(ok));
        assertFalse(record().matches(ko));
    }

    @Test
    void multipleCriteriaAllMustMatch() {
        RuntimeExceptionRecord pattern = new RuntimeExceptionRecord("orderProcessing", "validateAmount",
                type(java.lang.RuntimeException.class), null, 400, true, null);
        assertTrue(record().matches(pattern));

        RuntimeExceptionRecord oneWrong = new RuntimeExceptionRecord("orderProcessing", "validateAmount",
                type(java.lang.RuntimeException.class), null, 401, true, null);
        assertFalse(record().matches(oneWrong));
    }

    // --- matches(IRuntimeStepOnException) ---

    @Test
    void matchesOnExceptionConfiguration() {
        IRuntimeStepOnException onEx = new TestOnException("orderProcessing", "validateAmount",
                type(IllegalArgumentException.class));
        assertTrue(record().matches(onEx));
    }

    @Test
    void matchesOnExceptionFailsOnWrongStep() {
        IRuntimeStepOnException onEx = new TestOnException("orderProcessing", "otherStep",
                type(IllegalArgumentException.class));
        assertFalse(record().matches(onEx));
    }

    @Test
    void matchesOnExceptionRequiresAbort() {
        // onException always builds a pattern with hasAborted=true; a non-aborted record won't match
        RuntimeExceptionRecord nonAborted = new RuntimeExceptionRecord("orderProcessing", "validateAmount",
                type(IllegalArgumentException.class), new IllegalArgumentException("x"), 400, false, null);
        IRuntimeStepOnException onEx = new TestOnException("orderProcessing", "validateAmount",
                type(IllegalArgumentException.class));
        assertFalse(nonAborted.matches(onEx));
    }

    private record TestOnException(String runtimeName, String fromStep,
            IClass<? extends Throwable> exception) implements IRuntimeStepOnException {
    }
}
