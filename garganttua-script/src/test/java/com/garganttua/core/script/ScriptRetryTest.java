package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.context.ScriptContext;
import com.garganttua.core.script.functions.ScriptFunctions;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

class ScriptRetryTest {

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    private IScript createScript(String source) {
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

        IInjectionContext injectionContext = injectionContextBuilder.build();
        injectionContext.onInit().onStart();

        IExpressionContext expressionContext = expressionContextBuilder.build();

        ScriptContext ctx = new ScriptContext(expressionContext, injectionContext);
        ctx.load(source);
        ctx.compile();
        return ctx;
    }

    // ---- Time Unit Functions (Direct Java) ----

    @Test
    void testMilliseconds() {
        assertEquals(100L, ScriptFunctions.milliseconds(100));
        assertEquals(100L, ScriptFunctions.milliseconds(100L));
    }

    @Test
    void testSeconds() {
        assertEquals(10000L, ScriptFunctions.seconds(10));
        assertEquals(10000L, ScriptFunctions.seconds(10L));
    }

    @Test
    void testMinutes() {
        assertEquals(60000L, ScriptFunctions.minutes(1));
        assertEquals(120000L, ScriptFunctions.minutes(2L));
    }

    @Test
    void testHours() {
        assertEquals(3600000L, ScriptFunctions.hours(1));
        assertEquals(7200000L, ScriptFunctions.hours(2L));
    }

    // ---- Time Unit Functions in Script ----

    @Test
    void testSecondsInScript() {
        IScript s = createScript("result <- seconds(5)");
        s.execute();
        assertEquals(5000L, s.getVariable("result", Long.class).orElse(null));
    }

    @Test
    void testMinutesInScript() {
        IScript s = createScript("result <- minutes(2)");
        s.execute();
        assertEquals(120000L, s.getVariable("result", Long.class).orElse(null));
    }

    @Test
    void testMillisecondsInScript() {
        IScript s = createScript("result <- milliseconds(100)");
        s.execute();
        assertEquals(100L, s.getVariable("result", Long.class).orElse(null));
    }

    @Test
    void testHoursInScript() {
        IScript s = createScript("result <- hours(1)");
        s.execute();
        assertEquals(3600000L, s.getVariable("result", Long.class).orElse(null));
    }

    // ---- Retry with already evaluated value (returns immediately) ----

    @Test
    void testRetryWithDirectValue() {
        IScript s = createScript("""
                result <- retry(3, milliseconds(10), "direct-value")
                """);
        s.execute();
        assertEquals("direct-value", s.getVariable("result", String.class).orElse(null));
    }

    @Test
    void testRetryWithDirectInteger() {
        // Use string "42" since literals are typed strictly
        IScript s = createScript("""
                result <- retry(3, milliseconds(0), "42")
                """);
        s.execute();
        assertEquals("42", s.getVariable("result", String.class).orElse(null));
    }

    // ---- Retry With Backoff (already evaluated value) ----

    @Test
    void testRetryWithBackoffDirectValue() {
        IScript s = createScript("""
                result <- retryWithBackoff(3, milliseconds(10), milliseconds(100), "backoff-value")
                """);
        s.execute();
        assertEquals("backoff-value", s.getVariable("result", String.class).orElse(null));
    }

    // ---- Retry with Constructor Call (evaluated once) ----

    @Test
    void testRetryWithConstructorCall() {
        IScript s = createScript("""
                result <- retry(2, milliseconds(0), :(String.class, "constructed"))
                """);
        s.execute();
        assertEquals("constructed", s.getVariable("result", String.class).orElse(null));
    }

    // ---- Retry with Integer Constructor ----

    @Test
    void testRetryWithIntegerConstructor() {
        IScript s = createScript("""
                result <- retry(2, milliseconds(0), :(Integer.class, "123"))
                """);
        s.execute();
        assertEquals(123, s.getVariable("result", Integer.class).orElse(null));
    }

    // ---- Retry with null returns null ----

    @Test
    void testRetryWithNullReturnsNull() {
        // Test direct Java call
        assertNull(ScriptFunctions.retry(3, 10, null));
    }

    // ---- RetryWithBackoff with null returns null ----

    @Test
    void testRetryWithBackoffNullReturnsNull() {
        // Test direct Java call
        assertNull(ScriptFunctions.retryWithBackoff(3, 10, 100, null));
    }

    // ---- Retry validates maxAttempts ----

    @Test
    void testRetryInvalidMaxAttempts() {
        assertThrows(Exception.class, () -> ScriptFunctions.retry(0, 10, new FixedSupplierBuilder<>("value").build()));
    }

    // ---- Retry validates delay ----

    @Test
    void testRetryInvalidDelay() {
        assertThrows(Exception.class, () -> ScriptFunctions.retry(3, -1, new FixedSupplierBuilder<>("value").build()));
    }

    // ---- RetryWithBackoff validates parameters ----

    @Test
    void testRetryWithBackoffInvalidMaxAttempts() {
        assertThrows(Exception.class, () -> ScriptFunctions.retryWithBackoff(0, 10, 100, new FixedSupplierBuilder<>("value").build()));
    }

    @Test
    void testRetryWithBackoffInvalidDelay() {
        assertThrows(Exception.class, () -> ScriptFunctions.retryWithBackoff(3, -1, 100, new FixedSupplierBuilder<>("value").build()));
    }

    @Test
    void testRetryWithBackoffMaxDelayLessThanInitial() {
        assertThrows(Exception.class, () -> ScriptFunctions.retryWithBackoff(3, 100, 10, new FixedSupplierBuilder<>("value").build()));
    }
}
