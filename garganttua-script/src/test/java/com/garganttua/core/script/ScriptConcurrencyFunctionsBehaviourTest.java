package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.InterruptibleLeaseMutex;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.functions.ScriptConcurrencyFunctions;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for {@link ScriptConcurrencyFunctions#synchronizedExec} and
 * {@link ScriptConcurrencyFunctions#sync}, asserting validation guards, mode
 * handling, and that the supplied expression is executed inside the lock and
 * its result returned.
 */
class ScriptConcurrencyFunctionsBehaviourTest {

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

    private static IMutex mutex() {
        return new InterruptibleLeaseMutex("behaviour-test");
    }

    // ---- synchronizedExec validation ----

    @Test
    void synchronizedRejectsNullMutexName() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptConcurrencyFunctions.synchronizedExec(null, mutex(), "acquire", 0, supplier("x")));
        assertTrue(ex.getMessage().contains("mutexName cannot be null or blank"));
    }

    @Test
    void synchronizedRejectsBlankMutexName() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptConcurrencyFunctions.synchronizedExec("   ", mutex(), "acquire", 0, supplier("x")));
        assertTrue(ex.getMessage().contains("mutexName cannot be null or blank"));
    }

    @Test
    void synchronizedRejectsNullMutex() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptConcurrencyFunctions.synchronizedExec("m", null, "acquire", 0, supplier("x")));
        assertTrue(ex.getMessage().contains("mutex cannot be null"));
    }

    @Test
    void synchronizedRejectsNullMode() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptConcurrencyFunctions.synchronizedExec("m", mutex(), null, 0, supplier("x")));
        assertTrue(ex.getMessage().contains("mode cannot be null"));
    }

    @Test
    void synchronizedRejectsUnknownMode() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptConcurrencyFunctions.synchronizedExec("m", mutex(), "weird", 0, supplier("x")));
        assertTrue(ex.getMessage().contains("unknown mode 'weird'"));
    }

    @Test
    void synchronizedNullExpressionReturnsNull() {
        assertNull(ScriptConcurrencyFunctions.synchronizedExec("m", mutex(), "acquire", 0, null));
    }

    // ---- synchronizedExec behaviour ----

    @Test
    void synchronizedAcquireExecutesExpressionAndReturnsResult() {
        AtomicInteger calls = new AtomicInteger();
        ISupplier<Object> s = new ISupplier<>() {
            @Override
            public Optional<Object> supply() {
                calls.incrementAndGet();
                return Optional.of("inside-lock");
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
        };
        Object r = ScriptConcurrencyFunctions.synchronizedExec("m", mutex(), "acquire", 1000, s);
        assertEquals("inside-lock", r);
        assertEquals(1, calls.get());
    }

    @Test
    void synchronizedTryAcquireOnFreeMutexSucceeds() {
        Object r = ScriptConcurrencyFunctions.synchronizedExec("m", mutex(), "tryAcquire", 0, supplier("v"));
        assertEquals("v", r);
    }

    @Test
    void synchronizedModeIsCaseInsensitive() {
        Object r = ScriptConcurrencyFunctions.synchronizedExec("m", mutex(), "ACQUIRE", 0, supplier("v"));
        assertEquals("v", r);
    }

    // ---- sync ----

    @Test
    void syncRejectsNullMutexName() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptConcurrencyFunctions.sync(null, mutex(), supplier("x")));
        assertTrue(ex.getMessage().contains("mutexName cannot be null or blank"));
    }

    @Test
    void syncRejectsNullMutex() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptConcurrencyFunctions.sync("m", null, supplier("x")));
        assertTrue(ex.getMessage().contains("mutex cannot be null"));
    }

    @Test
    void syncNullExpressionReturnsNull() {
        assertNull(ScriptConcurrencyFunctions.sync("m", mutex(), null));
    }

    @Test
    void syncExecutesExpressionAndReturnsResult() {
        Object r = ScriptConcurrencyFunctions.sync("m", mutex(), supplier("done"));
        assertEquals("done", r);
    }

    @Test
    void syncTwiceOnSameMutexSequentiallySucceeds() {
        IMutex m = mutex();
        assertEquals("a", ScriptConcurrencyFunctions.sync("m", m, supplier("a")));
        // Lock fully released after first call, so a second acquisition must succeed.
        assertEquals("b", ScriptConcurrencyFunctions.sync("m", m, supplier("b")));
    }
}
