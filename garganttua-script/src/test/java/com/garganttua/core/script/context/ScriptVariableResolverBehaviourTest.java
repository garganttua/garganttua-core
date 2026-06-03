package com.garganttua.core.script.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.RuntimeContext;
import com.garganttua.core.runtime.RuntimeExpressionContext;

/**
 * Direct behaviour tests for {@link ScriptVariableResolver}, exercising the
 * special-name resolution ({@code $N} positional args, {@code code}, {@code output}),
 * type-assignability gating, and the no-context short-circuit, against a real
 * {@link RuntimeContext} bound through {@link RuntimeExpressionContext}.
 */
class ScriptVariableResolverBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;
    private final ScriptVariableResolver resolver = new ScriptVariableResolver();

    private static final IClass<Object> OBJECT = IClass.getClass(Object.class);
    private static final IClass<String> STRING = IClass.getClass(String.class);
    private static final IClass<Integer> INTEGER = IClass.getClass(Integer.class);

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

    private IInjectionContext injectionContext() {
        IInjectionContextBuilder ctx = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        return ctx.build();
    }

    private RuntimeContext<Object[], Object> startedContext(Object[] input) {
        RuntimeContext<Object[], Object> ctx = new RuntimeContext<>(
                injectionContext(), input, Object.class, Map.of(), UUID.randomUUID());
        ctx.onInit().onStart();
        return ctx;
    }

    // ---- no active context ----

    @Test
    void resolveWithoutContextReturnsEmpty() {
        // No RuntimeExpressionContext bound -> empty regardless of the name.
        assertTrue(resolver.resolve("anything", OBJECT).isEmpty());
        assertTrue(resolver.resolve("$0", OBJECT).isEmpty());
        assertTrue(resolver.resolve("code", INTEGER).isEmpty());
    }

    @Test
    void setVariableWithoutContextIsNoOp() {
        // Must not throw when there is no bound context.
        resolver.setVariable("x", "y");
    }

    // ---- positional arguments ($N) ----

    @Test
    void resolvePositionalArgReturnsTypedValue() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[] {"first", "second"});
        RuntimeExpressionContext.runIn(ctx, () -> {
            assertEquals("first", resolver.resolve("$0", STRING).orElse(null));
            assertEquals("second", resolver.resolve("$1", STRING).orElse(null));
        });
    }

    @Test
    void resolvePositionalArgAsObjectFallbackWhenNotInstance() {
        // Index 0 holds an Integer; requesting it typed as Object goes through the
        // OBJECT fallback branch (type.equals(OBJECT_CLASS)).
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[] {Integer.valueOf(7)});
        RuntimeExpressionContext.runIn(ctx, () ->
                assertEquals(7, resolver.resolve("$0", OBJECT).orElse(null)));
    }

    @Test
    void resolvePositionalArgWrongTypeReturnsEmpty() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[] {"a string"});
        RuntimeExpressionContext.runIn(ctx, () ->
                assertTrue(resolver.resolve("$0", INTEGER).isEmpty()));
    }

    @Test
    void resolvePositionalArgOutOfBoundsReturnsEmpty() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[] {"only one"});
        RuntimeExpressionContext.runIn(ctx, () -> {
            assertTrue(resolver.resolve("$5", OBJECT).isEmpty());
            assertTrue(resolver.resolve("$1", OBJECT).isEmpty());
        });
    }

    @Test
    void resolveNullPositionalArgReturnsEmpty() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[] {null, "x"});
        RuntimeExpressionContext.runIn(ctx, () ->
                assertTrue(resolver.resolve("$0", OBJECT).isEmpty()));
    }

    @Test
    void dollarNameThatIsNotNumericFallsThroughToVariableLookup() {
        // "$foo" is not a positional index (NumberFormatException) -> treated as a
        // normal variable name, which is absent -> empty.
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () ->
                assertTrue(resolver.resolve("$foo", OBJECT).isEmpty()));
    }

    // ---- special: code ----

    @Test
    void resolveCodeReturnsCurrentExitCode() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () -> {
            ctx.setCode(404);
            // code is requested as Integer (assignable from Integer).
            assertEquals(404, resolver.resolve("code", INTEGER).orElse(null));
            // Object is also assignable-from Integer.
            assertEquals(404, resolver.resolve("code", OBJECT).orElse(null));
        });
    }

    @Test
    void resolveCodeWithIncompatibleTypeReturnsEmpty() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () -> {
            ctx.setCode(1);
            // String is not assignable from Integer -> empty.
            assertTrue(resolver.resolve("code", STRING).isEmpty());
        });
    }

    // ---- special: output ----

    @Test
    void resolveOutputReturnsSetValue() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () -> {
            ctx.setOutput("the output");
            assertEquals("the output", resolver.resolve("output", STRING).orElse(null));
            assertEquals("the output", resolver.resolve("output", OBJECT).orElse(null));
        });
    }

    @Test
    void resolveOutputWhenUnsetReturnsEmpty() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () ->
                assertTrue(resolver.resolve("output", OBJECT).isEmpty()));
    }

    @Test
    void resolveOutputWrongTypeReturnsEmpty() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () -> {
            ctx.setOutput("a string output");
            assertTrue(resolver.resolve("output", INTEGER).isEmpty());
        });
    }

    // ---- ordinary variables + setVariable ----

    @Test
    void setAndResolveOrdinaryVariableRoundTrips() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () -> {
            resolver.setVariable("greeting", "hi");
            assertEquals("hi", resolver.resolve("greeting", STRING).orElse(null));
        });
    }

    @Test
    void setVariableOutputRoutesToContextOutput() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () -> {
            resolver.setVariable("output", "routed");
            // Routed to the context output, NOT stored as a named variable.
            assertEquals("routed", ctx.getOutput());
        });
    }

    @Test
    void setVariableOutputNullIsIgnored() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () -> {
            ctx.setOutput("kept");
            resolver.setVariable("output", null); // null output is dropped
            assertEquals("kept", ctx.getOutput());
        });
    }

    @Test
    void resolveMissingOrdinaryVariableReturnsEmpty() {
        IRuntimeContext<Object[], Object> ctx = startedContext(new Object[0]);
        RuntimeExpressionContext.runIn(ctx, () ->
                assertTrue(resolver.resolve("never-set", OBJECT).isEmpty()));
    }
}
