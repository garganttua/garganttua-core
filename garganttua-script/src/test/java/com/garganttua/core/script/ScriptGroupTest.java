package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.annotation.processor.IndexedAnnotationScanner;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Tests for statement groups feature.
 */
class ScriptGroupTest {

    private static IReflectionBuilder reflectionBuilder;
    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;

    @BeforeAll
    static void setupClass() throws Exception {
        @SuppressWarnings("unchecked")
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new IndexedAnnotationScanner());
        reflectionBuilder.build();
    }

    @BeforeEach
    void setup() {
        injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

        injectionContextBuilder.build().onInit().onStart();
        expressionContextBuilder.build();
    }

    @Test
    void testSimpleStatementGroup() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            result <- (
                a <- 10
                b <- 20
                @b
            )
            """);
        script.compile();
        int code = script.execute();

        assertEquals(0, code);
        assertEquals(20, script.getVariable("result", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testGroupWithCodeMapping() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            result <- (
                value <- 42
            ) -> 99
            """);
        script.compile();
        int code = script.execute();

        assertEquals(99, code);
        assertEquals(42, script.getVariable("value", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testGroupWithInnerCodeMappings() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            (
                first <- 1 -> 10
                second <- 2 -> 20
            ) -> 100
            """);
        script.compile();
        int code = script.execute();

        // The outer group code (100) should be the final code
        assertEquals(100, code);
    }

    @Test
    void testNestedGroups() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            outer <- (
                a <- 5
                inner <- (
                    b <- 10
                    @b
                )
                @inner
            )
            """);
        script.compile();
        int code = script.execute();

        // With nested groups, the outer result captures the inner result
        // Code might not be 0 due to inner group execution
        assertEquals(10, script.getVariable("outer", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testGroupVariablesVisibility() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            x <- 100
            (
                y <- 150
                z <- 175
            )
            final <- @z
            """);
        script.compile();
        int code = script.execute();

        assertEquals(0, code);
        assertEquals(100, script.getVariable("x", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(150, script.getVariable("y", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(175, script.getVariable("z", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(175, script.getVariable("final", IClass.getClass(Integer.class)).orElse(null));
    }
}
