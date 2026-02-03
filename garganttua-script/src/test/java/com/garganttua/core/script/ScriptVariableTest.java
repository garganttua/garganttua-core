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

class ScriptVariableTest {

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

    // ---- Result assignment (<-) stores evaluated value ----

    @Test
    void testResultAssignmentStoresValue() {
        IScript s = createScript("result <- string(\"hello\")");
        s.execute();
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("hello", val.get());
    }

    @Test
    void testResultAssignmentStoresStringLiteral() {
        IScript s = createScript("result <- \"world\"");
        s.execute();
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("world", val.get());
    }

    // ---- Expression assignment (=) stores IExpression (lazy) ----

    @Test
    void testExpressionAssignmentStoresNonNull() {
        IScript s = createScript("result = string(\"hello\")");
        s.execute();
        // With '=', the IExpression object is stored, not the evaluated result
        Optional<Object> val = s.getVariable("result", Object.class);
        assertTrue(val.isPresent());
        // The stored value should be an IExpression, not a String
        assertFalse(val.get() instanceof String);
    }

    // ---- Variable with code return ----

    @Test
    void testVariableWithCodeReturn() {
        IScript s = createScript("x <- string(\"world\") -> 200");
        int code = s.execute();
        assertEquals(200, code);
        Optional<String> val = s.getVariable("x", String.class);
        assertTrue(val.isPresent());
        assertEquals("world", val.get());
    }

    // ---- Multiple variables across steps ----

    @Test
    void testMultipleVariables() {
        IScript s = createScript("""
                a <- string("first")
                b <- string("second")
                """);
        s.execute();
        assertEquals("first", s.getVariable("a", String.class).orElse(null));
        assertEquals("second", s.getVariable("b", String.class).orElse(null));
    }

    // ---- Variable overwritten ----

    @Test
    void testVariableOverwritten() {
        IScript s = createScript("""
                x <- string("old")
                x <- string("new")
                """);
        s.execute();
        assertEquals("new", s.getVariable("x", String.class).orElse(null));
    }

    // ---- Variable not available before execute ----

    @Test
    void testNoVariableBeforeExecute() {
        IScript s = createScript("x <- string(\"hello\")");
        Optional<String> val = s.getVariable("x", String.class);
        assertTrue(val.isEmpty());
    }

    // ---- Variable set in catch handler ----

    @Test
    void testVariableAfterCatch() {
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => err <- "caught-error" -> 400
                """);
        int code = s.execute();
        assertEquals(400, code);
        assertEquals("caught-error", s.getVariable("err", String.class).orElse(null));
    }

    // ---- Variable not set when expression throws ----

    @Test
    void testVariableNotSetOnException() {
        IScript s = createScript("""
                x <- class("nonexistent.Foo")
                """);
        s.execute();
        assertTrue(s.getVariable("x", Object.class).isEmpty());
    }

    // ---- Type mismatch returns empty ----

    @Test
    void testVariableTypeMismatch() {
        IScript s = createScript("x <- string(\"hello\")");
        s.execute();
        assertTrue(s.getVariable("x", Integer.class).isEmpty());
        assertTrue(s.getVariable("x", String.class).isPresent());
    }

    // ---- Non-existent variable returns empty ----

    @Test
    void testNonExistentVariable() {
        IScript s = createScript("x <- string(\"hello\")");
        s.execute();
        assertTrue(s.getVariable("y", String.class).isEmpty());
    }

    // ---- Variable set in pipe handler ----

    @Test
    void testVariableInPipeHandler() {
        IScript s = createScript("""
                string("hello")
                | => piped <- "pipe-value" -> 300
                """);
        int code = s.execute();
        assertEquals(300, code);
        assertEquals("pipe-value", s.getVariable("piped", String.class).orElse(null));
    }

    // ---- Variable set in downstream catch handler ----

    @Test
    void testVariableInDownstreamCatch() {
        IScript s = createScript("""
                "step0" -> 100
                * => caught <- "downstream-value" -> 600
                class("nonexistent.Foo")
                """);
        int code = s.execute();
        assertEquals(600, code);
        assertEquals("downstream-value", s.getVariable("caught", String.class).orElse(null));
    }
}
