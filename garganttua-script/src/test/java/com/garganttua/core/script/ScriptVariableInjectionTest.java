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
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.context.ScriptContext;

class ScriptVariableInjectionTest {

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

    @Test
    void testVariableInjectedAsArgument() {
        IScript s = createScript("x <- string(\"hello\")\nresult <- string(@x)");
        s.execute();
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("hello", val.get());
    }

    @Test
    void testVariableInjectedMultipleTimes() {
        IScript s = createScript("x <- string(\"world\")\ny <- string(@x)\nresult <- string(@y)");
        s.execute();
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("world", val.get());
    }

    @Test
    void testVariableOverwrittenThenInjected() {
        IScript s = createScript("x <- string(\"first\")\nx <- string(\"second\")\nresult <- string(@x)");
        s.execute();
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("second", val.get());
    }

    @Test
    void testVariableInjectedInEqualsCondition() {
        IScript s = createScript("x <- string(\"hello\")\ny <- string(\"hello\")\nresult <- equals(@x, @y)");
        s.execute();
        Optional<Boolean> val = s.getVariable("result", Boolean.class);
        assertTrue(val.isPresent());
        assertTrue(val.get());
    }

    @Test
    void testVariableInjectedInNotEqualsCondition() {
        IScript s = createScript("x <- string(\"hello\")\ny <- string(\"world\")\nresult <- notEquals(@x, @y)");
        s.execute();
        Optional<Boolean> val = s.getVariable("result", Boolean.class);
        assertTrue(val.isPresent());
        assertTrue(val.get());
    }

    @Test
    void testVariableInjectedInChainedSteps() {
        IScript s = createScript("a <- string(\"test\")\nb <- string(@a)\nresult <- string(@b)");
        s.execute();
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("test", val.get());
    }

    @Test
    void testVariableInjectedWithCodeReturn() {
        IScript s = createScript("x <- string(\"ok\") -> 200\nresult <- string(@x)");
        int code = s.execute();
        assertEquals(200, code);
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("ok", val.get());
    }

    @Test
    void testVariableInjectedInNotNull() {
        IScript s = createScript("x <- string(\"value\")\nresult <- notNull(@x)");
        s.execute();
        Optional<Boolean> val = s.getVariable("result", Boolean.class);
        assertTrue(val.isPresent());
        assertTrue(val.get());
    }
}
