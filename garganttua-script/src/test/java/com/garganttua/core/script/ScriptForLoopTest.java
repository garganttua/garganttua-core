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
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;

class ScriptForLoopTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        @SuppressWarnings("unchecked")
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

        IRuntimesBuilder runtimesBuilder = RuntimesBuilder.builder().provide(injectionContextBuilder);
        ScriptContext ctx = new ScriptContext(expressionContext, runtimesBuilder, null);
        ctx.load(source);
        ctx.compile();
        return ctx;
    }

    @Test
    void testSimpleForLoop() {
        IScript s = createScript(
                "counter <- 0\n" +
                "for(\"counter\", increment(@counter), lower(@counter, 5), @counter)");
        s.execute();
        Optional<Object> val = s.getVariable("counter", IClass.getClass(Object.class));
        assertTrue(val.isPresent());
        assertEquals(5, ((Number) val.get()).intValue());
    }

    @Test
    void testForLoopConditionFalseFromStart() {
        IScript s = createScript(
                "counter <- 9\n" +
                "for(\"counter\", increment(@counter), lower(@counter, 5), @counter)");
        s.execute();
        Optional<Object> val = s.getVariable("counter", IClass.getClass(Object.class));
        assertTrue(val.isPresent());
        assertEquals(9, ((Number) val.get()).intValue());
    }

    @Test
    void testForLoopModifiesVariable() {
        IScript s = createScript(
                "counter <- 0\n" +
                "for(\"counter\", increment(@counter), lower(@counter, 3), @counter)");
        s.execute();
        Optional<Object> val = s.getVariable("counter", IClass.getClass(Object.class));
        assertTrue(val.isPresent());
        assertEquals(3, ((Number) val.get()).intValue());
    }

    @Test
    void testForLoopWithDecrement() {
        IScript s = createScript(
                "counter <- 10\n" +
                "for(\"counter\", decrement(@counter), greater(@counter, 5), @counter)");
        s.execute();
        Optional<Object> val = s.getVariable("counter", IClass.getClass(Object.class));
        assertTrue(val.isPresent());
        assertEquals(5, ((Number) val.get()).intValue());
    }
}
