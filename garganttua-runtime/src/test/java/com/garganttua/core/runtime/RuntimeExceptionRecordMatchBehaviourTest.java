package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Behaviour tests for {@link RuntimeExceptionRecord#matches}: wildcard/null-field
 * semantics, polymorphic exception-type matching, and the
 * {@link IRuntimeStepOnException} overload (which forces {@code hasAborted=true}).
 * Also covers the {@link RuntimeStepCatch} / {@link RuntimeStepOnException}
 * record accessors and {@code exceptionMessage()}.
 */
class RuntimeExceptionRecordMatchBehaviourTest {

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

    private RuntimeExceptionRecord record(String rt, String step,
            Class<? extends Throwable> type, Throwable ex, Integer code, Boolean aborted) {
        return new RuntimeExceptionRecord(rt, step,
                type == null ? null : IClass.getClass(type), ex, code, aborted, "ref");
    }

    // -------------------------------------------------------------------------
    // null pattern
    // -------------------------------------------------------------------------

    @Test
    void matches_nullPattern_returnsFalse() {
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class,
                new java.lang.RuntimeException("x"), 1, true);
        assertFalse(r.matches((RuntimeExceptionRecord) null));
    }

    // -------------------------------------------------------------------------
    // all-null pattern = wildcard => matches everything
    // -------------------------------------------------------------------------

    @Test
    void matches_allWildcardPattern_returnsTrue() {
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class,
                new java.lang.RuntimeException("x"), 1, true);
        RuntimeExceptionRecord wildcard = new RuntimeExceptionRecord(null, null, null, null, null, null, null);
        assertTrue(r.matches(wildcard));
    }

    // -------------------------------------------------------------------------
    // runtimeName / stepName exact match
    // -------------------------------------------------------------------------

    @Test
    void matches_runtimeNameMismatch_returnsFalse() {
        RuntimeExceptionRecord r = record("rtA", "s", RuntimeException.class, null, null, null);
        assertFalse(r.matches(record("rtB", null, null, null, null, null)));
    }

    @Test
    void matches_stepNameMismatch_returnsFalse() {
        RuntimeExceptionRecord r = record("rt", "stepA", RuntimeException.class, null, null, null);
        assertFalse(r.matches(record(null, "stepB", null, null, null, null)));
    }

    @Test
    void matches_runtimeAndStepNameMatch_returnsTrue() {
        RuntimeExceptionRecord r = record("rt", "step", RuntimeException.class, null, null, null);
        assertTrue(r.matches(record("rt", "step", null, null, null, null)));
    }

    // -------------------------------------------------------------------------
    // polymorphic exception type matching
    // -------------------------------------------------------------------------

    @Test
    void matches_patternSupertype_matchesRecordSubtype() {
        // record holds IllegalStateException; pattern expects java.lang.RuntimeException (assignable from)
        RuntimeExceptionRecord r = record("rt", "s", IllegalStateException.class, null, null, null);
        assertTrue(r.matches(record(null, null, java.lang.RuntimeException.class, null, null, null)));
    }

    @Test
    void matches_patternSubtype_doesNotMatchRecordSupertype() {
        // record holds java.lang.RuntimeException; pattern expects IllegalStateException -> not assignable
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class, null, null, null);
        assertFalse(r.matches(record(null, null, IllegalStateException.class, null, null, null)));
    }

    @Test
    void matches_patternExpectsType_butRecordHasNoType_returnsFalse() {
        RuntimeExceptionRecord r = record("rt", "s", null, null, null, null);
        assertFalse(r.matches(record(null, null, java.lang.RuntimeException.class, null, null, null)));
    }

    // -------------------------------------------------------------------------
    // code & hasAborted exact match
    // -------------------------------------------------------------------------

    @Test
    void matches_codeMismatch_returnsFalse() {
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class, null, 500, null);
        assertFalse(r.matches(record(null, null, null, null, 400, null)));
    }

    @Test
    void matches_hasAbortedMismatch_returnsFalse() {
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class, null, null, false);
        assertFalse(r.matches(record(null, null, null, null, null, true)));
    }

    @Test
    void matches_codeAndAbortedMatch_returnsTrue() {
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class, null, 200, true);
        assertTrue(r.matches(record(null, null, null, null, 200, true)));
    }

    // -------------------------------------------------------------------------
    // matches(IRuntimeStepOnException) — forces hasAborted=true in pattern
    // -------------------------------------------------------------------------

    @Test
    void matchesOnException_abortingRecordWithMatchingType_returnsTrue() {
        RuntimeExceptionRecord aborting = record("rt", "step", IllegalArgumentException.class,
                new IllegalArgumentException("x"), 1, true);
        IRuntimeStepOnException onEx = new RuntimeStepOnException(
                IClass.getClass(IllegalArgumentException.class), "rt", "step");
        assertTrue(aborting.matches(onEx));
    }

    @Test
    void matchesOnException_nonAbortingRecord_returnsFalse() {
        // The onException overload builds a pattern with hasAborted=true, so a
        // non-aborting record can never satisfy it.
        RuntimeExceptionRecord nonAborting = record("rt", "step", IllegalArgumentException.class,
                new IllegalArgumentException("x"), 1, false);
        IRuntimeStepOnException onEx = new RuntimeStepOnException(
                IClass.getClass(IllegalArgumentException.class), "rt", "step");
        assertFalse(nonAborting.matches(onEx));
    }

    @Test
    void matchesOnException_polymorphicType_andWildcardLocation() {
        RuntimeExceptionRecord aborting = record("rt", "step", IllegalArgumentException.class,
                new IllegalArgumentException("x"), 1, true);
        // onException expecting java.lang.RuntimeException (supertype) and null runtime/step (wildcards)
        IRuntimeStepOnException onEx = new RuntimeStepOnException(
                IClass.getClass(java.lang.RuntimeException.class), null, null);
        assertTrue(aborting.matches(onEx));
    }

    @Test
    void matchesOnException_stepMismatch_returnsFalse() {
        RuntimeExceptionRecord aborting = record("rt", "stepA", IllegalArgumentException.class,
                new IllegalArgumentException("x"), 1, true);
        IRuntimeStepOnException onEx = new RuntimeStepOnException(
                IClass.getClass(IllegalArgumentException.class), "rt", "stepB");
        assertFalse(aborting.matches(onEx));
    }

    // -------------------------------------------------------------------------
    // exceptionMessage()
    // -------------------------------------------------------------------------

    @Test
    void exceptionMessage_returnsUnderlyingMessage() {
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class,
                new java.lang.RuntimeException("kaboom"), 1, true);
        assertEquals("kaboom", r.exceptionMessage());
    }

    @Test
    void exceptionMessage_nullException_throwsNpe() {
        RuntimeExceptionRecord r = record("rt", "s", RuntimeException.class, null, 1, true);
        assertThrows(NullPointerException.class, r::exceptionMessage);
    }

    // -------------------------------------------------------------------------
    // RuntimeStepCatch / RuntimeStepOnException record accessors
    // -------------------------------------------------------------------------

    @Test
    void runtimeStepCatch_accessors() {
        IClass<? extends Throwable> type = IClass.getClass(IllegalStateException.class);
        RuntimeStepCatch c = new RuntimeStepCatch(type, 503);
        assertEquals(type, c.exception());
        assertEquals(503, c.code());
    }

    @Test
    void runtimeStepOnException_accessors() {
        IClass<? extends Throwable> type = IClass.getClass(IllegalStateException.class);
        RuntimeStepOnException o = new RuntimeStepOnException(type, "myRuntime", "myStep");
        assertEquals(type, o.exception());
        assertEquals("myRuntime", o.runtimeName());
        assertEquals("myStep", o.fromStep());
    }
}
