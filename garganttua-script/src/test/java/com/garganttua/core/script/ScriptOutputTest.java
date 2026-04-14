package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
import com.garganttua.core.script.functions.LogFunctions;
import com.garganttua.core.script.functions.ScriptFunctions;

class ScriptOutputTest {

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

        ScriptContext ctx = new ScriptContext(expressionContext, injectionContext, null);
        ctx.load(source);
        ctx.compile();
        return ctx;
    }

    // ========== Direct Java Tests for eprint/eprintln ==========

    @Test
    void testEprintWritesToStderr() {
        PrintStream originalErr = System.err;
        try {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setErr(new PrintStream(capture));

            String result = ScriptFunctions.eprint("error message");

            assertEquals("error message", result);
            assertTrue(capture.toString().contains("error message"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testEprintNullValue() {
        PrintStream originalErr = System.err;
        try {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setErr(new PrintStream(capture));

            String result = ScriptFunctions.eprint(null);

            assertEquals("null", result);
            assertTrue(capture.toString().contains("null"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testEprintlnIsAliasForEprint() {
        PrintStream originalErr = System.err;
        try {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setErr(new PrintStream(capture));

            String result = ScriptFunctions.eprintln("test");

            assertEquals("test", result);
            assertTrue(capture.toString().contains("test"));
        } finally {
            System.setErr(originalErr);
        }
    }

    // ========== Direct Java Tests for format ==========

    @Test
    void testFormatOneArg() {
        assertEquals("Hello world", ScriptFunctions.format("Hello %s", "world"));
    }

    @Test
    void testFormatTwoArgs() {
        assertEquals("Hello world 42", ScriptFunctions.format("Hello %s %d", "world", 42));
    }

    @Test
    void testFormatThreeArgs() {
        assertEquals("a-b-c", ScriptFunctions.format("%s-%s-%s", "a", "b", "c"));
    }

    @Test
    void testFormatNullPattern() {
        assertEquals("null", ScriptFunctions.format(null, "arg"));
    }

    @Test
    void testFormatNullArg() {
        assertEquals("Value: null", ScriptFunctions.format("Value: %s", (Object) null));
    }

    // ========== Direct Java Tests for log functions ==========

    @Test
    void testLogInfoReturnsMessage() {
        String result = LogFunctions.logInfo("test message");
        assertEquals("test message", result);
    }

    @Test
    void testLogDebugReturnsMessage() {
        String result = LogFunctions.logDebug("debug msg");
        assertEquals("debug msg", result);
    }

    @Test
    void testLogWarnReturnsMessage() {
        String result = LogFunctions.logWarn("warning");
        assertEquals("warning", result);
    }

    @Test
    void testLogErrorReturnsMessage() {
        String result = LogFunctions.logError("error");
        assertEquals("error", result);
    }

    @Test
    void testLogTraceReturnsMessage() {
        String result = LogFunctions.logTrace("trace");
        assertEquals("trace", result);
    }

    @Test
    void testLogNullMessage() {
        assertEquals("null", LogFunctions.logInfo(null));
    }

    @Test
    void testLogWithArgReturnsPattern() {
        String result = LogFunctions.logInfo("Processing {}", "item-1");
        assertEquals("Processing {}", result);
    }

    // ========== Script Integration Tests ==========

    @Test
    void testPrintInScript() {
        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setOut(new PrintStream(capture));

            IScript s = createScript("result <- print(\"hello from script\")");
            s.execute();

            assertEquals("hello from script", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
            assertTrue(capture.toString().contains("hello from script"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testEprintInScript() {
        PrintStream originalErr = System.err;
        try {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setErr(new PrintStream(capture));

            IScript s = createScript("result <- eprint(\"stderr message\")");
            s.execute();

            assertEquals("stderr message", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
            assertTrue(capture.toString().contains("stderr message"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testFormatInScript() {
        IScript s = createScript("""
                name <- "world"
                result <- format("Hello %s", @name)
                """);
        s.execute();
        assertEquals("Hello world", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testFormatWithMultipleArgsInScript() {
        IScript s = createScript("""
                a <- "x"
                b <- "y"
                result <- format("%s and %s", @a, @b)
                """);
        s.execute();
        assertEquals("x and y", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testLogInfoInScript() {
        IScript s = createScript("result <- log_info(\"info message\")");
        s.execute();
        assertEquals("info message", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testLogDebugInScript() {
        IScript s = createScript("result <- log_debug(\"debug message\")");
        s.execute();
        assertEquals("debug message", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testLogWarnInScript() {
        IScript s = createScript("result <- log_warn(\"warning message\")");
        s.execute();
        assertEquals("warning message", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testLogErrorInScript() {
        IScript s = createScript("result <- log_error(\"error message\")");
        s.execute();
        assertEquals("error message", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testLogWithFormatInScript() {
        IScript s = createScript("""
                msg <- format("Processed %s items", "42")
                result <- log_info(@msg)
                """);
        s.execute();
        assertEquals("Processed 42 items", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testFormatWithIntegerConversionInScript() {
        IScript s = createScript("""
                count <- 42
                msg <- format("Items: %s", string(@count))
                result <- log_info(@msg)
                """);
        s.execute();
        assertEquals("Items: 42", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testPrintAndFormatCombined() {
        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setOut(new PrintStream(capture));

            IScript s = createScript("""
                    name <- "Garganttua"
                    version <- "2.0"
                    msg <- format("%s v%s started", @name, @version)
                    result <- print(@msg)
                    """);
            s.execute();

            assertEquals("Garganttua v2.0 started", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
            assertTrue(capture.toString().contains("Garganttua v2.0 started"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
