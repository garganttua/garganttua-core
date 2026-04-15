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
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;
import com.garganttua.core.script.functions.ScriptFunctions;

class CastTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new ReflectionsAnnotationScanner());
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

        ScriptContext ctx = new ScriptContext(expressionContext, injectionContextBuilder, null);
        ctx.load(source);
        ctx.compile();
        return ctx;
    }

    @Test
    void testCastStringValue() {
        IScript s = createScript("result <- cast(String.Class, \"hello\")\n:length(@result)");
        s.execute();
        Optional<String> val = s.getVariable("result", IClass.getClass(String.class));
        assertTrue(val.isPresent());
        assertEquals("hello", val.get());
    }

    @Test
    void testCastCompatibleType() {
        IScript s = createScript("x <- \"world\"\nresult <- cast(Object.Class, @x)");
        s.execute();
        Optional<Object> val = s.getVariable("result", IClass.getClass(Object.class));
        assertTrue(val.isPresent());
        assertEquals("world", val.get());
    }
}
