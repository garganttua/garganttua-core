package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.functions.ScriptResilienceFunctions;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for {@link ScriptResilienceFunctions#retry} and
 * {@link ScriptResilienceFunctions#retryWithBackoff}, using counting suppliers to
 * assert exact attempt counts, success/failure outcomes and parameter validation.
 */
class ScriptResilienceFunctionsBehaviourTest {

    /** A supplier that fails the first {@code failuresBeforeSuccess} times, then returns {@code value}. */
    private static ISupplier<Object> failThenSucceed(int failuresBeforeSuccess, Object value, AtomicInteger calls) {
        return new ISupplier<>() {
            @Override
            public Optional<Object> supply() throws SupplyException {
                int n = calls.incrementAndGet();
                if (n <= failuresBeforeSuccess) {
                    throw new IllegalStateException("fail #" + n);
                }
                return Optional.ofNullable(value);
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
        };
    }

    // ---- retry validation ----

    @Test
    void retryRejectsZeroMaxAttempts() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptResilienceFunctions.retry(0, 0, null));
        assertTrue(ex.getMessage().contains("maxAttempts must be >= 1"));
    }

    @Test
    void retryRejectsNegativeDelay() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptResilienceFunctions.retry(1, -1, null));
        assertTrue(ex.getMessage().contains("delay cannot be negative"));
    }

    @Test
    void retryNullSupplierReturnsNull() {
        assertNull(ScriptResilienceFunctions.retry(3, 0, null));
    }

    // ---- retry behaviour ----

    @Test
    void retrySucceedsFirstAttemptCallsOnce() {
        AtomicInteger calls = new AtomicInteger();
        Object r = ScriptResilienceFunctions.retry(3, 0, failThenSucceed(0, "ok", calls));
        assertEquals("ok", r);
        assertEquals(1, calls.get());
    }

    @Test
    void retrySucceedsOnThirdAttempt() {
        AtomicInteger calls = new AtomicInteger();
        Object r = ScriptResilienceFunctions.retry(5, 0, failThenSucceed(2, "recovered", calls));
        assertEquals("recovered", r);
        assertEquals(3, calls.get(), "should stop calling once it succeeds");
    }

    @Test
    void retryExhaustsAllAttemptsThenThrowsWithLastMessage() {
        AtomicInteger calls = new AtomicInteger();
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptResilienceFunctions.retry(3, 0, failThenSucceed(100, "never", calls)));
        assertEquals(3, calls.get(), "should attempt exactly maxAttempts times");
        assertTrue(ex.getMessage().contains("all 3 attempts failed"));
        assertTrue(ex.getMessage().contains("fail #3"));
    }

    @Test
    void retrySucceedsImmediatelyWithDelayButNoSleepNeeded() {
        AtomicInteger calls = new AtomicInteger();
        long start = System.currentTimeMillis();
        Object r = ScriptResilienceFunctions.retry(3, 1000, failThenSucceed(0, "fast", calls));
        long elapsed = System.currentTimeMillis() - start;
        assertEquals("fast", r);
        assertTrue(elapsed < 500, "no delay when first attempt succeeds, took " + elapsed);
    }

    // ---- retryWithBackoff validation ----

    @Test
    void backoffRejectsZeroMaxAttempts() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptResilienceFunctions.retryWithBackoff(0, 0, 0, null));
        assertTrue(ex.getMessage().contains("maxAttempts must be >= 1"));
    }

    @Test
    void backoffRejectsNegativeInitialDelay() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptResilienceFunctions.retryWithBackoff(1, -1, 100, null));
        assertTrue(ex.getMessage().contains("initialDelay cannot be negative"));
    }

    @Test
    void backoffRejectsMaxDelaySmallerThanInitial() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptResilienceFunctions.retryWithBackoff(1, 100, 50, null));
        assertTrue(ex.getMessage().contains("maxDelay must be >= initialDelay"));
    }

    @Test
    void backoffNullSupplierReturnsNull() {
        assertNull(ScriptResilienceFunctions.retryWithBackoff(3, 0, 0, null));
    }

    // ---- retryWithBackoff behaviour ----

    @Test
    void backoffSucceedsOnSecondAttempt() {
        AtomicInteger calls = new AtomicInteger();
        Object r = ScriptResilienceFunctions.retryWithBackoff(5, 0, 0, failThenSucceed(1, "v", calls));
        assertEquals("v", r);
        assertEquals(2, calls.get());
    }

    @Test
    void backoffExhaustsAttemptsThenThrows() {
        AtomicInteger calls = new AtomicInteger();
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptResilienceFunctions.retryWithBackoff(2, 0, 0, failThenSucceed(100, null, calls)));
        assertEquals(2, calls.get());
        assertTrue(ex.getMessage().contains("all 2 attempts failed"));
    }
}
