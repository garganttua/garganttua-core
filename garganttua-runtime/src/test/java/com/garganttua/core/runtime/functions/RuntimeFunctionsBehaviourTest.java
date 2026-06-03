package com.garganttua.core.runtime.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.RuntimeContext;
import com.garganttua.core.runtime.RuntimeExpressionContext;

/**
 * Behaviour tests for {@link RuntimeFunctions#runtimeContext()}: returns the
 * bound context inside a scope and throws {@link ExpressionException} outside one.
 */
class RuntimeFunctionsBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;

    @BeforeAll
    static void setup() throws Exception {
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    private RuntimeContext<String, String> ctx() {
        IInjectionContextBuilder builder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        return new RuntimeContext<>(builder.build(), "in", String.class, Map.of(), UUID.randomUUID());
    }

    @Test
    void runtimeContext_outsideScope_throwsExpressionException() {
        ExpressionException ex = assertThrows(ExpressionException.class, RuntimeFunctions::runtimeContext);
        assertEquals("No runtime context available. Ensure this is called within a runtime execution.",
                ex.getMessage());
    }

    @Test
    void runtimeContext_insideScope_returnsBoundContext() {
        IRuntimeContext<String, String> ctx = ctx();
        IRuntimeContext<?, ?> seen = RuntimeExpressionContext.callIn(ctx,
                RuntimeFunctions::runtimeContext);
        assertSame(ctx, seen);
    }

    @Test
    void runtimeContext_afterScopeEnds_throwsAgain() {
        IRuntimeContext<String, String> ctx = ctx();
        RuntimeExpressionContext.callIn(ctx, RuntimeFunctions::runtimeContext);
        // Scope has ended; the function must fail again.
        assertThrows(ExpressionException.class, RuntimeFunctions::runtimeContext);
    }
}
