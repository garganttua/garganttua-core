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

    // ---- Reserved Variables Tests ----

    @Test
    void testOutputVariable() {
        // Test setting @output variable
        IScript s = createScript("output <- \"my output value\" -> 100");
        int code = s.execute();
        assertEquals(100, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("my output value", s.getOutput().get());
    }

    @Test
    void testCodeVariableInCatch() {
        // Test that @code is accessible in catch handlers
        // First set code to 200, then catch exception and verify @code is available
        IScript s = createScript("""
                "setup" -> 200
                class("nonexistent.Foo")
                ! => @code -> 500
                """);
        int code = s.execute();
        // The catch handler should use @code (200) as the exit code, but -> 500 overrides it
        assertEquals(500, code);
    }

    @Test
    void testExceptionAndMessageVariablesInCatch() {
        // Test that @exception and @message are accessible in catch handlers
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => print(@message) -> 400
                """);
        int code = s.execute();
        assertEquals(400, code);
    }

    @Test
    void testOutputVariableReturnedByScript() {
        // Test that output set via @output is returned via getOutput()
        IScript s = createScript("""
                output <- "result from script"
                "done" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("result from script", s.getOutput().get());
    }

    @Test
    void testCodeVariableAccess() {
        // Test reading @code variable during execution
        IScript s = createScript("""
                "first" -> 100
                @code -> 200
                """);
        int code = s.execute();
        // The second statement reads @code (100) and uses it as expression result, then -> 200
        assertEquals(200, code);
    }

    @Test
    void testCodeVariableWithConcatenate() {
        // Test that @code (Object type) works with concatenate(String, String)
        // This tests the type compatibility fix for Object-typed variables
        IScript s = createScript("""
                "setup" -> 200
                class("nonexistent.Foo")
                ! => print(concatenate("code was ", @code)) -> 500
                """);
        int code = s.execute();
        assertEquals(500, code);
    }

    @Test
    void testMessageVariableWithConcatenate() {
        // Test that @message (Object type) works with string functions
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => print(concatenate("Error: ", @message)) -> 400
                """);
        int code = s.execute();
        assertEquals(400, code);
    }

    // ---- Time Function Tests ----

    @Test
    void testTimeFunction() {
        // Test that time() returns a numeric value (execution time)
        IScript s = createScript("""
                expr = string("hello")
                elapsed <- time(@expr)
                "done" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
    }

    @Test
    void testTimeFunctionWithDirectValue() {
        // Test that time() with direct value returns near-zero
        IScript s = createScript("""
                elapsed <- time("direct value")
                "done" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
    }

    // ---- Eager Evaluation (.variable) Tests ----

    @Test
    void testEagerEvaluationDotSyntax() {
        // Test that .variable evaluates stored expression
        IScript s = createScript("""
                expr = string("hello")
                result <- .expr
                "done" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
    }

    @Test
    void testEagerEvaluationInFunctionCall() {
        // Test that .variable works as function argument
        IScript s = createScript("""
                expr = string("world")
                print(concatenate("hello ", .expr))
                "done" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
    }

    @Test
    void testEagerEvaluationWithNonSupplierValue() {
        // Test that .variable on a non-supplier value just returns the value
        IScript s = createScript("""
                value <- "already evaluated"
                result <- .value
                "done" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
    }

    @Test
    void testLazyExpressionNotEvaluatedOnAssignment() {
        // Test that = assignment does NOT evaluate the expression
        // The print should only happen when .expr is called
        IScript s = createScript("""
                expr = print("evaluated")
                "not evaluated yet" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
        // Note: "evaluated" should NOT be printed here since we never call .expr
    }

    @Test
    void testEagerEvaluationTriggersExecution() {
        // Test that .expr triggers the stored expression
        IScript s = createScript("""
                expr = print("now evaluated")
                .expr
                "done" -> 0
                """);
        int code = s.execute();
        assertEquals(0, code);
        // "now evaluated" should be printed
    }

    // ---- Exception handling (captured, not thrown) ----

    @Test
    void testExceptionIsCapturedNotThrown() {
        // Test that exceptions are captured in the result and not propagated
        // call() with non-existent script will throw ExpressionException
        IScript s = createScript("""
                call("nonexistent-script")
                """);

        // Should NOT throw an exception - exceptions are captured
        int code = s.execute();

        // Code should be an error code (50 is GENERIC_RUNTIME_ERROR_CODE)
        assertEquals(50, code);

        // Exception should be captured
        assertTrue(s.hasAborted());
        assertTrue(s.getLastException().isPresent());
        assertTrue(s.getLastExceptionMessage().isPresent());
        assertTrue(s.getLastExceptionMessage().get().contains("nonexistent-script"));
    }

    @Test
    void testSuccessfulExecutionNoException() {
        IScript s = createScript("""
                result <- "success"
                """);

        int code = s.execute();
        assertEquals(0, code);

        // No exception should be captured
        assertFalse(s.hasAborted());
        assertTrue(s.getLastException().isEmpty());
        assertTrue(s.getLastExceptionMessage().isEmpty());
    }

    @Test
    void testExceptionResetBetweenExecutions() {
        // Test that exception state is reset between executions
        IScript s = createScript("""
                result <- "success"
                """);

        // First execution - success
        int code1 = s.execute();
        assertEquals(0, code1);
        assertFalse(s.hasAborted());

        // The script is already compiled, so we create a new one for the error test
        IScript s2 = createScript("""
                call("nonexistent-script")
                """);

        // Second execution - error
        int code2 = s2.execute();
        assertEquals(50, code2);
        assertTrue(s2.hasAborted());

        // Third execution of first script - success again (reset state)
        int code3 = s.execute();
        assertEquals(0, code3);
        assertFalse(s.hasAborted());
        assertTrue(s.getLastException().isEmpty());
    }
}
