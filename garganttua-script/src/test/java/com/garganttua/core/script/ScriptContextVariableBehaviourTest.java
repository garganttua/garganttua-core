package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Behaviour tests for {@link ScriptContext#getVariable(String, IClass)} typed
 * retrieval semantics and cross-execution state (output / exception / abort
 * flag), driven through real compiled scripts.
 */
class ScriptContextVariableBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    @SuppressWarnings("unchecked")
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

        ScriptContext ctx = new ScriptContext(expressionContext,
                () -> RuntimesBuilder.builder().provide(injectionContextBuilder), null);
        ctx.load(source);
        ctx.compile();
        return ctx;
    }

    // ---- getVariable typed retrieval ----

    @Test
    void getVariableReturnsValueWhenTypeMatches() {
        IScript s = createScript("greeting <- \"hello\" -> 0");
        s.execute();
        assertEquals("hello", s.getVariable("greeting", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void getVariableAsSupertypeMatches() {
        IScript s = createScript("greeting <- \"hello\" -> 0");
        s.execute();
        // String IS-A Object, so retrieval typed as Object must succeed.
        assertEquals("hello", s.getVariable("greeting", IClass.getClass(Object.class)).orElse(null));
    }

    @Test
    void getVariableWrongTypeReturnsEmpty() {
        IScript s = createScript("greeting <- \"hello\" -> 0");
        s.execute();
        // The value is a String, not an Integer -> empty rather than a ClassCastException.
        assertTrue(s.getVariable("greeting", IClass.getClass(Integer.class)).isEmpty());
    }

    @Test
    void getVariableMissingNameReturnsEmpty() {
        IScript s = createScript("greeting <- \"hello\" -> 0");
        s.execute();
        assertTrue(s.getVariable("doesNotExist", IClass.getClass(String.class)).isEmpty());
    }

    @Test
    void getVariableNumericTypePreserved() {
        IScript s = createScript("n <- seconds(3) -> 0");
        s.execute();
        assertEquals(3000L, s.getVariable("n", IClass.getClass(Long.class)).orElse(null));
        // A Long is not a String, so the wrong-type lookup must be empty.
        assertTrue(s.getVariable("n", IClass.getClass(String.class)).isEmpty());
    }

    // ---- output / exception cross-execution state ----

    @Test
    void outputAbsentBeforeExecution() {
        IScript s = createScript("output <- \"set\" -> 0");
        assertTrue(s.getOutput().isEmpty(), "output must be empty until the script runs");
        s.execute();
        assertEquals("set", s.getOutput().orElse(null));
    }

    @Test
    void lastExceptionMessageReportsRootCause() {
        IScript s = createScript("call(\"nonexistent-script\")");
        int code = s.execute();
        assertEquals(50, code);
        assertTrue(s.hasAborted());
        assertTrue(s.getLastExceptionMessage().isPresent());
        assertTrue(s.getLastExceptionMessage().get().contains("nonexistent-script"));
    }

    @Test
    void successfulReexecutionClearsPriorErrorState() {
        // Run an aborting script first.
        IScript bad = createScript("call(\"nope\")");
        assertEquals(50, bad.execute());
        assertTrue(bad.hasAborted());

        // A fresh, successful script must report a clean state.
        IScript ok = createScript("result <- \"fine\" -> 0");
        assertEquals(0, ok.execute());
        assertFalse(ok.hasAborted());
        assertTrue(ok.getLastException().isEmpty());

        // Re-running the same successful script keeps it clean (state reset per run).
        assertEquals(0, ok.execute());
        assertFalse(ok.hasAborted());
    }
}
