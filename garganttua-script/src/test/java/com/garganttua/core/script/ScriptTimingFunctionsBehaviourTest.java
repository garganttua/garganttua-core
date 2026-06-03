package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.functions.ScriptTimingFunctions;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for {@link ScriptTimingFunctions} static methods, exercised
 * directly with hand-built {@link ISupplier} instances so the timing, null and
 * failure paths can be asserted with concrete expected values.
 */
class ScriptTimingFunctionsBehaviourTest {

    /** A minimal supplier wrapping a fixed value (or throwing). */
    private static ISupplier<Object> supplier(Object value) {
        return new ISupplier<>() {
            @Override
            public Optional<Object> supply() throws SupplyException {
                return Optional.ofNullable(value);
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
        };
    }

    private static ISupplier<Object> throwingSupplier(RuntimeException toThrow) {
        return new ISupplier<>() {
            @Override
            public Optional<Object> supply() throws SupplyException {
                throw toThrow;
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
        };
    }

    // ---- time() ----

    @Test
    void timeReturnsZeroForNullSupplier() {
        assertEquals(0L, ScriptTimingFunctions.time(null));
    }

    @Test
    void timeInvokesSupplierExactlyOnce() {
        AtomicInteger calls = new AtomicInteger();
        ISupplier<Object> s = new ISupplier<>() {
            @Override
            public Optional<Object> supply() {
                calls.incrementAndGet();
                return Optional.of("x");
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
        };
        long elapsed = ScriptTimingFunctions.time(s);
        assertEquals(1, calls.get());
        assertTrue(elapsed >= 0L, "elapsed should be non-negative");
    }

    @Test
    void timeMeasuresAtLeastSleepDuration() {
        ISupplier<Object> sleeping = new ISupplier<>() {
            @Override
            public Optional<Object> supply() throws SupplyException {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Optional.of("done");
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
        };
        long elapsed = ScriptTimingFunctions.time(sleeping);
        assertTrue(elapsed >= 35L, "expected >= ~40ms, got " + elapsed);
    }

    @Test
    void timeWrapsSupplierFailureInExpressionException() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptTimingFunctions.time(throwingSupplier(new RuntimeException("boom"))));
        assertTrue(ex.getMessage().contains("boom"));
        assertTrue(ex.getMessage().contains("time:"));
    }

    // ---- timeWithResult() ----

    @Test
    void timeWithResultNullSupplierReturnsZeroAndNull() {
        Object[] r = ScriptTimingFunctions.timeWithResult(null);
        assertEquals(2, r.length);
        assertEquals(0L, r[0]);
        assertNull(r[1]);
    }

    @Test
    void timeWithResultReturnsElapsedAndValue() {
        Object[] r = ScriptTimingFunctions.timeWithResult(supplier("hello"));
        assertEquals(2, r.length);
        assertInstanceOf(Long.class, r[0]);
        assertTrue((Long) r[0] >= 0L);
        assertEquals("hello", r[1]);
    }

    @Test
    void timeWithResultEmptyOptionalProducesNullResult() {
        Object[] r = ScriptTimingFunctions.timeWithResult(supplier(null));
        assertNull(r[1]);
    }

    @Test
    void timeWithResultWrapsFailureWithElapsedInMessage() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptTimingFunctions.timeWithResult(throwingSupplier(new IllegalStateException("bad"))));
        assertTrue(ex.getMessage().contains("timeWithResult:"));
        assertTrue(ex.getMessage().contains("bad"));
    }

    // ---- time-unit conversions ----

    @Test
    void millisecondsIdentity() {
        assertEquals(0L, ScriptTimingFunctions.milliseconds(0));
        assertEquals(250L, ScriptTimingFunctions.milliseconds(250));
        assertEquals(250L, ScriptTimingFunctions.milliseconds(250L));
        assertEquals(-5L, ScriptTimingFunctions.milliseconds(-5));
    }

    @Test
    void secondsConvertToMillis() {
        assertEquals(0L, ScriptTimingFunctions.seconds(0));
        assertEquals(1000L, ScriptTimingFunctions.seconds(1));
        assertEquals(90000L, ScriptTimingFunctions.seconds(90L));
    }

    @Test
    void minutesConvertToMillis() {
        assertEquals(60000L, ScriptTimingFunctions.minutes(1));
        assertEquals(300000L, ScriptTimingFunctions.minutes(5L));
    }

    @Test
    void hoursConvertToMillis() {
        assertEquals(3600000L, ScriptTimingFunctions.hours(1));
        assertEquals(86400000L, ScriptTimingFunctions.hours(24L));
    }
}
