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
import com.garganttua.core.aot.annotation.scanner.IndexedAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;

class ComparisonConditionTest {

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
    void testLower() {
        IScript s = createScript("a <- string(\"3\")\nb <- string(\"5\")\nresult <- lower(@a, @b)");
        s.execute();
        assertEquals(true, s.getVariable("result", IClass.getClass(Boolean.class)).orElse(null));
    }

    @Test
    void testLowerFalse() {
        IScript s = createScript("a <- string(\"5\")\nb <- string(\"3\")\nresult <- lower(@a, @b)");
        s.execute();
        assertEquals(false, s.getVariable("result", IClass.getClass(Boolean.class)).orElse(null));
    }

    @Test
    void testGreater() {
        IScript s = createScript("a <- string(\"5\")\nb <- string(\"3\")\nresult <- greater(@a, @b)");
        s.execute();
        assertEquals(true, s.getVariable("result", IClass.getClass(Boolean.class)).orElse(null));
    }

    @Test
    void testGreaterFalse() {
        IScript s = createScript("a <- string(\"3\")\nb <- string(\"5\")\nresult <- greater(@a, @b)");
        s.execute();
        assertEquals(false, s.getVariable("result", IClass.getClass(Boolean.class)).orElse(null));
    }

    @Test
    void testLowerOrEquals() {
        IScript s = createScript("a <- string(\"3\")\nb <- string(\"3\")\nresult <- lowerOrEquals(@a, @b)");
        s.execute();
        assertEquals(true, s.getVariable("result", IClass.getClass(Boolean.class)).orElse(null));
    }

    @Test
    void testGreaterOrEquals() {
        IScript s = createScript("a <- string(\"5\")\nb <- string(\"5\")\nresult <- greaterOrEquals(@a, @b)");
        s.execute();
        assertEquals(true, s.getVariable("result", IClass.getClass(Boolean.class)).orElse(null));
    }

    @Test
    void testIncrement() {
        IScript s = createScript("a <- string(\"5\")\nresult <- increment(@a)");
        s.execute();
        Optional<Object> val = s.getVariable("result", IClass.getClass(Object.class));
        assertTrue(val.isPresent());
        assertEquals(6, ((Number) val.get()).intValue());
    }

    @Test
    void testDecrement() {
        IScript s = createScript("a <- string(\"5\")\nresult <- decrement(@a)");
        s.execute();
        Optional<Object> val = s.getVariable("result", IClass.getClass(Object.class));
        assertTrue(val.isPresent());
        assertEquals(4, ((Number) val.get()).intValue());
    }
}
