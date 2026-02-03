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

class ScriptContextTest {

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

    // ---- Expression-only statement (no variable, no code) ----

    @Test
    void testSimpleExpression() {
        IScript s = createScript("\"hello\"");
        int code = s.execute();
        assertEquals(0, code);
    }

    @Test
    void testIntLiteralExpression() {
        IScript s = createScript("42");
        int code = s.execute();
        assertEquals(0, code);
    }

    // ---- Variable assignment with '=' (stores IExpression) ----

    @Test
    void testExpressionAssignment() {
        IScript s = createScript("result = \"hello\"");
        int code = s.execute();
        assertEquals(0, code);
    }

    // ---- Variable assignment with '<-' (stores result) ----

    @Test
    void testResultAssignment() {
        IScript s = createScript("result <- \"hello\"");
        int code = s.execute();
        assertEquals(0, code);
    }

    // ---- Code return with '->' ----

    @Test
    void testCodeReturn() {
        IScript s = createScript("\"hello\" -> 200");
        int code = s.execute();
        assertEquals(200, code);
    }

    // ---- Full form: var = expression -> code ----

    @Test
    void testFullFormWithEquals() {
        IScript s = createScript("result = \"hello\" -> 201");
        int code = s.execute();
        assertEquals(201, code);
    }

    @Test
    void testFullFormWithArrow() {
        IScript s = createScript("result <- \"hello\" -> 201");
        int code = s.execute();
        assertEquals(201, code);
    }

    // ---- Multiple statements ----

    @Test
    void testMultipleStatements() {
        IScript s = createScript("""
                "first" -> 100
                "second" -> 200
                """);
        int code = s.execute();
        assertEquals(200, code);
    }

    // ---- Comments ----

    @Test
    void testComments() {
        IScript s = createScript("""
                // This is a comment
                "hello" -> 200
                /* multi-line
                   comment */
                """);
        int code = s.execute();
        assertEquals(200, code);
    }

    // ---- Function call expression ----

    @Test
    void testConstructorCallExpression() {
        IScript s = createScript(":(String.class, \"hello\")");
        int code = s.execute();
        assertEquals(0, code);
    }

    @Test
    void testConstructorCallWithVariableAndCode() {
        IScript s = createScript("result = :(String.class, \"hello\") -> 200");
        int code = s.execute();
        assertEquals(200, code);
    }

    // ---- Syntax error ----

    @Test
    void testSyntaxError() {
        assertThrows(ScriptException.class, () -> createScript("!"));
    }

    // ---- No script loaded / compiled ----

    @Test
    void testExecuteWithoutCompile() {
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true).withPackage("com.garganttua.core.runtime");
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);
        injectionContextBuilder.build().onInit().onStart();
        IExpressionContext expressionContext = expressionContextBuilder.build();
        ScriptContext ctx = new ScriptContext(expressionContext, injectionContextBuilder.build());
        assertThrows(ScriptException.class, () -> ctx.execute());
    }

    @Test
    void testCompileWithoutLoad() {
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true).withPackage("com.garganttua.core.runtime");
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);
        injectionContextBuilder.build().onInit().onStart();
        IExpressionContext expressionContext = expressionContextBuilder.build();
        ScriptContext ctx = new ScriptContext(expressionContext, injectionContextBuilder.build());
        assertThrows(ScriptException.class, () -> ctx.compile());
    }
}
