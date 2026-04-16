package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

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
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;

class ScriptFunctionTest {

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

    // ---- Basic function definition and call ----

    @Test
    void testFunctionNoParams() {
        IScript s = createScript(
                "greet = () => (\n" +
                "    result <- \"hello\"\n" +
                ")\n" +
                "output <- greet()");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("hello", s.getOutput().get());
    }

    @Test
    void testFunctionWithParameters() {
        IScript s = createScript(
                "greetUser = (name) => (\n" +
                "    result <- concatenate(\"hello \", @name)\n" +
                ")\n" +
                "output <- greetUser(\"world\")");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("hello world", s.getOutput().get());
    }

    // ---- Function call with expression arguments ----

    @Test
    void testFunctionCallWithExpressionArgs() {
        IScript s = createScript(
                "wrap = (val) => (\n" +
                "    result <- concatenate(\"[\", @val, \"]\")\n" +
                ")\n" +
                "output <- wrap(concatenate(\"a\", \"b\"))");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("[ab]", s.getOutput().get());
    }

    // ---- Function call with variable arguments ----

    @Test
    void testFunctionCallWithVariableArgs() {
        IScript s = createScript(
                "wrap = (val) => (\n" +
                "    result <- concatenate(\"[\", @val, \"]\")\n" +
                ")\n" +
                "v <- \"test\"\n" +
                "output <- wrap(@v)");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("[test]", s.getOutput().get());
    }

    // ---- Multiple function calls ----

    @Test
    void testMultipleFunctionCalls() {
        IScript s = createScript(
                "prefix = (val) => (\n" +
                "    result <- concatenate(\">\", @val)\n" +
                ")\n" +
                "x <- prefix(\"a\")\n" +
                "y <- prefix(\"b\")\n" +
                "output <- concatenate(@x, @y)");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals(">a>b", s.getOutput().get());
    }

    // ---- Function with multiple statements in body ----

    @Test
    void testFunctionMultipleStatements() {
        IScript s = createScript(
                "transform = (a, b) => (\n" +
                "    joined <- concatenate(@a, @b)\n" +
                "    result <- concatenate(\"[\", @joined, \"]\")\n" +
                ")\n" +
                "output <- transform(\"hello\", \" world\")");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("[hello world]", s.getOutput().get());
    }

    // ---- Parameter scope isolation ----

    @Test
    void testParameterScopeIsolation() {
        IScript s = createScript(
                "x <- \"original\"\n" +
                "f = (x) => (\n" +
                "    result <- concatenate(@x, \"!\")\n" +
                ")\n" +
                "f(\"temp\")\n" +
                "output <- @x");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        // x should be restored to "original" after function call
        assertEquals("original", s.getOutput().get());
    }

    // ---- Function defined and called inside the same group ----

    @Test
    void testFunctionInGroup() {
        IScript s = createScript(
                "(\n" +
                "    wrap = (n) => (\n" +
                "        result <- concatenate(\"(\", @n, \")\")\n" +
                "    )\n" +
                "    output <- wrap(\"ok\")\n" +
                ")");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("(ok)", s.getOutput().get());
    }

    // ---- Function defined in a group is NOT visible outside (scope isolation) ----

    @Test
    void testFunctionScopeIsolation() {
        IScript s = createScript(
                "(\n" +
                "    localFunc = (n) => (\n" +
                "        result <- concatenate(\"[\", @n, \"]\")\n" +
                "    )\n" +
                "    inner <- localFunc(\"ok\")\n" +
                ")\n" +
                "output <- @inner");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("[ok]", s.getOutput().get());
    }

    // ---- Calling one function from another ----

    @Test
    void testFunctionCallingFunction() {
        IScript s = createScript(
                "bracket = (x) => (\n" +
                "    result <- concatenate(\"[\", @x, \"]\")\n" +
                ")\n" +
                "doubleBracket = (x) => (\n" +
                "    inner <- bracket(@x)\n" +
                "    result <- bracket(@inner)\n" +
                ")\n" +
                "output <- doubleBracket(\"ok\")");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("[[ok]]", s.getOutput().get());
    }

    // ---- Function with two parameters ----

    @Test
    void testFunctionTwoParams() {
        IScript s = createScript(
                "join = (a, b) => (\n" +
                "    result <- concatenate(@a, \"-\", @b)\n" +
                ")\n" +
                "output <- join(\"left\", \"right\")");
        int code = s.execute();
        assertEquals(0, code);
        assertTrue(s.getOutput().isPresent());
        assertEquals("left-right", s.getOutput().get());
    }
}
