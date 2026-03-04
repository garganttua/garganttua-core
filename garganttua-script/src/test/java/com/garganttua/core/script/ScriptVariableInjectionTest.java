package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.annotation.processor.IndexedAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;

class ScriptVariableInjectionTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        @SuppressWarnings("unchecked")
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new IndexedAnnotationScanner());
        reflectionBuilder.build();
    }

    private IScript createScript(String source) {
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
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

    @Test
    void testVariableInjectedAsArgument() {
        IScript s = createScript("x <- string(\"hello\")\nresult <- string(@x)");
        s.execute();
        Optional<String> val = s.getVariable("result", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("hello", val.get());
    }

    @Test
    void testVariableInjectedMultipleTimes() {
        IScript s = createScript("x <- string(\"world\")\ny <- string(@x)\nresult <- string(@y)");
        s.execute();
        Optional<String> val = s.getVariable("result", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("world", val.get());
    }

    @Test
    void testVariableOverwrittenThenInjected() {
        IScript s = createScript("x <- string(\"first\")\nx <- string(\"second\")\nresult <- string(@x)");
        s.execute();
        Optional<String> val = s.getVariable("result", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("second", val.get());
    }

    @Test
    void testVariableInjectedInEqualsCondition() {
        IScript s = createScript("x <- string(\"hello\")\ny <- string(\"hello\")\nresult <- equals(@x, @y)");
        s.execute();
        Optional<Boolean> val = s.getVariable("result", IClass.getClass(Boolean.class));
        assertTrue(val.isPresent());
        assertTrue(val.get());
    }

    @Test
    void testVariableInjectedInNotEqualsCondition() {
        IScript s = createScript("x <- string(\"hello\")\ny <- string(\"world\")\nresult <- notEquals(@x, @y)");
        s.execute();
        Optional<Boolean> val = s.getVariable("result", IClass.getClass(Boolean.class));
        assertTrue(val.isPresent());
        assertTrue(val.get());
    }

    @Test
    void testVariableInjectedInChainedSteps() {
        IScript s = createScript("a <- string(\"test\")\nb <- string(@a)\nresult <- string(@b)");
        s.execute();
        Optional<String> val = s.getVariable("result", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("test", val.get());
    }

    @Test
    void testVariableInjectedWithCodeReturn() {
        IScript s = createScript("x <- string(\"ok\") -> 200\nresult <- string(@x)");
        int code = s.execute();
        assertEquals(200, code);
        Optional<String> val = s.getVariable("result", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("ok", val.get());
    }

    @Test
    void testVariableInjectedInNotNull() {
        IScript s = createScript("x <- string(\"value\")\nresult <- notNull(@x)");
        s.execute();
        Optional<Boolean> val = s.getVariable("result", IClass.getClass(Boolean.class));
        assertTrue(val.isPresent());
        assertTrue(val.get());
    }
}
