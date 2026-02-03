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

/**
 * Tests for script arguments (@0, @1, @2...) and conditional pipe clauses.
 * Based on the script:
 * <pre>
 * var <- "hello"
 * print(@var) -> 200
 * print(@0)
 * print(@1)
 * print(@2) -> 201
 * | equals(@0, @1) => print("coucou") -> 202
 * </pre>
 */
class ScriptArgumentsAndPipeTest {

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

    // =====================================================================
    // VARIABLE ASSIGNMENT AND PRINT
    // =====================================================================

    @Test
    void testVariableAssignmentAndPrint() {
        IScript s = createScript("""
                var <- "hello"
                print(@var) -> 200
                """);
        int code = s.execute();
        assertEquals(200, code);
        Optional<String> val = s.getVariable("var", String.class);
        assertTrue(val.isPresent());
        assertEquals("hello", val.get());
    }

    // =====================================================================
    // SCRIPT ARGUMENTS (@0, @1, @2...)
    // =====================================================================

    @Test
    void testPrintScriptArguments() {
        IScript s = createScript("""
                print(@0)
                print(@1)
                print(@2) -> 201
                """);
        int code = s.execute("salut", "bonjour", "hello");
        assertEquals(201, code);
    }

    @Test
    void testPrintScriptArgumentsWithMissingArgs() {
        IScript s = createScript("""
                print(@0) -> 100
                """);
        // With no arguments, @0 should be null
        int code = s.execute();
        assertEquals(100, code);
    }

    @Test
    void testScriptArgumentValue() {
        IScript s = createScript("""
                result <- @0
                """);
        s.execute("test-value");
        Optional<String> val = s.getVariable("result", String.class);
        assertTrue(val.isPresent());
        assertEquals("test-value", val.get());
    }

    @Test
    void testMultipleScriptArguments() {
        IScript s = createScript("""
                arg0 <- @0
                arg1 <- @1
                arg2 <- @2
                """);
        s.execute("first", "second", "third");
        assertEquals("first", s.getVariable("arg0", String.class).orElse(null));
        assertEquals("second", s.getVariable("arg1", String.class).orElse(null));
        assertEquals("third", s.getVariable("arg2", String.class).orElse(null));
    }

    // =====================================================================
    // CONDITIONAL PIPE WITH equals() - pipes must be on separate lines
    // =====================================================================

    @Test
    void testConditionalPipeWithEqualsTrue() {
        IScript s = createScript("""
                print(@2) -> 201
                | equals(@0, @1) => print("coucou") -> 202
                """);
        int code = s.execute("same", "same", "arg2");
        // equals(@0, @1) is true (both are "same"), so pipe should match and return 202
        assertEquals(202, code);
    }

    @Test
    void testConditionalPipeWithEqualsFalse() {
        IScript s = createScript("""
                print(@2) -> 201
                | equals(@0, @1) => print("coucou") -> 202
                """);
        int code = s.execute("different1", "different2", "arg2");
        // equals(@0, @1) is false, so pipe should not match and return 201
        assertEquals(201, code);
    }

    @Test
    void testDefaultPipeClause() {
        IScript s = createScript("""
                print("hello") -> 100
                | => print("piped") -> 200
                """);
        int code = s.execute();
        // Default pipe always matches
        assertEquals(200, code);
    }

    // =====================================================================
    // FULL SCRIPT TEST
    // =====================================================================

    @Test
    void testFullScriptWithVariablesArgumentsAndConditionalPipe() {
        IScript s = createScript("""
                var <- "hello"
                print(@var) -> 200
                print(@0)
                print(@1)
                print(@2) -> 201
                | equals(@0, @1) => print("coucou") -> 202
                """);
        int code = s.execute("salut", "salut", "salut");
        // Last step has pipe with equals(@0, @1) which is true (both "salut"), so code should be 202
        assertEquals(202, code);

        // Verify variable was set
        Optional<String> val = s.getVariable("var", String.class);
        assertTrue(val.isPresent());
        assertEquals("hello", val.get());
    }

    @Test
    void testFullScriptWithDifferentArguments() {
        IScript s = createScript("""
                var <- "hello"
                print(@var) -> 200
                print(@0)
                print(@1)
                print(@2) -> 201
                | equals(@0, @1) => print("coucou") -> 202
                """);
        // Different arguments - pipe condition is false
        int code = s.execute("arg0", "arg1", "arg2");
        assertEquals(201, code);
    }

    // =====================================================================
    // MULTIPLE CONDITIONAL PIPES
    // =====================================================================

    @Test
    void testMultipleConditionalPipesFirstMatchWins() {
        IScript s = createScript("""
                "result" -> 100
                | equals(@0, @1) => "first" -> 201
                | equals(@0, @2) => "second" -> 202
                | => "default" -> 203
                """);
        int code = s.execute("test", "other", "test");
        // First condition false (test != other), second true (test == test) -> 202
        assertEquals(202, code);
    }

    @Test
    void testConditionalPipeWithDefaultFallback() {
        IScript s = createScript("""
                "result" -> 100
                | equals(@0, @1) => "nope" -> 201
                | equals(@1, @2) => "nope2" -> 202
                | => "default" -> 203
                """);
        int code = s.execute("one", "two", "three");
        // All conditions false, default matches -> 203
        assertEquals(203, code);
    }

    @Test
    void testPipeNotEvaluatedOnException() {
        // Expression throws, catch handles it -> pipe is never evaluated
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => "caught" -> 400
                | => "should-not-pipe" -> 300
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testNoPipeMatchesKeepsOriginalCode() {
        // All conditions false, no default -> pipe does nothing, original code preserved
        IScript s = createScript("""
                "hello" -> 100
                | equals(@0, @1) => "nope" -> 200
                """);
        assertEquals(100, s.execute("one", "two"));
    }

    // =====================================================================
    // MIXED VARIABLES AND ARGUMENTS
    // =====================================================================

    @Test
    void testMixedVariablesAndArguments() {
        IScript s = createScript("""
                myVar <- "hello"
                result <- equals(@myVar, @0)
                """);
        s.execute("hello");
        Optional<Boolean> val = s.getVariable("result", Boolean.class);
        assertTrue(val.isPresent());
        assertTrue(val.get()); // "hello" == "hello"
    }

    @Test
    void testMixedVariablesAndArgumentsFalse() {
        IScript s = createScript("""
                myVar <- "hello"
                result <- equals(@myVar, @0)
                """);
        s.execute("world");
        Optional<Boolean> val = s.getVariable("result", Boolean.class);
        assertTrue(val.isPresent());
        assertFalse(val.get()); // "hello" != "world"
    }
}
