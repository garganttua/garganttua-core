package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Behaviour tests for {@link RuntimeExpressionContext}: the ScopedValue-backed
 * binding semantics of {@code get()}, {@code runIn(...)} and {@code callIn(...)}.
 *
 * <p>Real {@link RuntimeContext} instances are used as identity tokens (the same
 * build+provide idiom as {@code RuntimeContextBehaviourTest}); only their
 * identity is asserted, never their behaviour.</p>
 */
class RuntimeExpressionContextBehaviourTest {

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

    // -------------------------------------------------------------------------
    // get() outside any scope
    // -------------------------------------------------------------------------

    @Test
    void get_outsideAnyScope_returnsNull() {
        assertNull(RuntimeExpressionContext.get());
    }

    // -------------------------------------------------------------------------
    // runIn
    // -------------------------------------------------------------------------

    @Test
    void runIn_bindsContextForBodyAndUnbindsAfter() {
        IRuntimeContext<String, String> ctx = ctx();
        AtomicReference<IRuntimeContext<?, ?>> seen = new AtomicReference<>();

        RuntimeExpressionContext.runIn(ctx, () -> seen.set(RuntimeExpressionContext.get()));

        assertSame(ctx, seen.get(), "body must observe the bound context");
        assertNull(RuntimeExpressionContext.get(), "binding must be cleared after runIn returns");
    }

    @Test
    void runIn_propagatesCheckedExceptionFromBody_andStillUnbinds() {
        IRuntimeContext<String, String> ctx = ctx();
        IOException boom = new IOException("io-failure");

        IOException thrown = assertThrows(IOException.class,
                () -> RuntimeExpressionContext.runIn(ctx, () -> {
                    throw boom;
                }));

        assertSame(boom, thrown);
        assertNull(RuntimeExpressionContext.get(), "binding cleared even when body throws");
    }

    // -------------------------------------------------------------------------
    // callIn
    // -------------------------------------------------------------------------

    @Test
    void callIn_returnsBodyResultWithContextBound() {
        IRuntimeContext<String, String> ctx = ctx();

        String result = RuntimeExpressionContext.callIn(ctx, () -> {
            assertSame(ctx, RuntimeExpressionContext.get());
            return "computed";
        });

        assertEquals("computed", result);
        assertNull(RuntimeExpressionContext.get());
    }

    @Test
    void callIn_propagatesRuntimeExceptionUnchanged() {
        IRuntimeContext<String, String> ctx = ctx();
        IllegalStateException boom = new IllegalStateException("nope");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> RuntimeExpressionContext.<String, RuntimeException>callIn(ctx, () -> {
                    throw boom;
                }));

        assertSame(boom, thrown);
        assertNull(RuntimeExpressionContext.get());
    }

    @Test
    void callIn_propagatesCheckedExceptionUnchanged() {
        IRuntimeContext<String, String> ctx = ctx();
        IOException boom = new IOException("checked-failure");

        IOException thrown = assertThrows(IOException.class,
                () -> RuntimeExpressionContext.<String, IOException>callIn(ctx, () -> {
                    throw boom;
                }));

        assertSame(boom, thrown);
    }

    // -------------------------------------------------------------------------
    // Nesting: inner binding restores outer on return
    // -------------------------------------------------------------------------

    @Test
    void nestedScopes_innerBindingRestoresOuterContextOnReturn() {
        IRuntimeContext<String, String> outer = ctx();
        IRuntimeContext<String, String> inner = ctx();
        AtomicReference<IRuntimeContext<?, ?>> seenInner = new AtomicReference<>();
        AtomicReference<IRuntimeContext<?, ?>> seenAfterInner = new AtomicReference<>();

        RuntimeExpressionContext.runIn(outer, () -> {
            assertSame(outer, RuntimeExpressionContext.get());
            RuntimeExpressionContext.runIn(inner, () -> seenInner.set(RuntimeExpressionContext.get()));
            seenAfterInner.set(RuntimeExpressionContext.get());
        });

        assertSame(inner, seenInner.get(), "inner scope sees inner context");
        assertSame(outer, seenAfterInner.get(), "outer context restored after inner scope ends");
        assertNull(RuntimeExpressionContext.get());
    }

    @Test
    void nestedScopes_exceptionInInnerStillRestoresOuter() {
        IRuntimeContext<String, String> outer = ctx();
        IRuntimeContext<String, String> inner = ctx();
        AtomicReference<IRuntimeContext<?, ?>> seenAfterInner = new AtomicReference<>();

        RuntimeExpressionContext.runIn(outer, () -> {
            try {
                RuntimeExpressionContext.runIn(inner, () -> {
                    throw new RuntimeException("inner-boom");
                });
            } catch (RuntimeException expected) {
                // swallow — verify outer is still bound after the inner scope unwound
            }
            seenAfterInner.set(RuntimeExpressionContext.get());
        });

        assertSame(outer, seenAfterInner.get());
    }

    @Test
    void callIn_canReturnNull() {
        IRuntimeContext<String, String> ctx = ctx();
        Object result = RuntimeExpressionContext.callIn(ctx, () -> null);
        assertNull(result);
    }

    @Test
    void callIn_resultObservesBoundContextOnly_notLeakedAcrossCalls() {
        IRuntimeContext<String, String> first = ctx();
        IRuntimeContext<String, String> second = ctx();

        IRuntimeContext<?, ?> seenFirst = RuntimeExpressionContext.callIn(first,
                RuntimeExpressionContext::get);
        IRuntimeContext<?, ?> seenSecond = RuntimeExpressionContext.callIn(second,
                RuntimeExpressionContext::get);

        assertSame(first, seenFirst);
        assertSame(second, seenSecond);
        assertNull(RuntimeExpressionContext.get());
    }
}
