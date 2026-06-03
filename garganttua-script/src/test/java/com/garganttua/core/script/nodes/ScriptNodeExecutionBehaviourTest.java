package com.garganttua.core.script.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
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
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for context-bound AST execution: {@link StatementBlock#execute()},
 * {@link ScriptFunction#invoke(Object...)} (scope isolation + arity / no-context
 * guards) and {@link CatchClauseHandler#tryCatchClauses}. Each runs against a real
 * {@link RuntimeContext} bound via {@link RuntimeExpressionContext}.
 */
class ScriptNodeExecutionBehaviourTest {

    private static IReflectionBuilder reflectionBuilder;

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

    private RuntimeContext<Object[], Object> startedContext() {
        RuntimeContext<Object[], Object> ctx = new RuntimeContext<>(
                injectionContext(), new Object[0], Object.class, Map.of(), UUID.randomUUID());
        ctx.onInit().onStart();
        return ctx;
    }

    /** A constant expression producing {@code value}. */
    private static IExpression<Object, ISupplier<Object>> constExpr(Object value) {
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() throws SupplyException { return Optional.ofNullable(value); }
                    @Override
                    public Type getSuppliedType() { return Object.class; }
                    @Override
                    public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
                };
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
            @Override
            public boolean isContextual() { return false; }
        };
    }

    /** An expression whose supplier throws the given runtime exception. */
    private static IExpression<Object, ISupplier<Object>> throwing(RuntimeException ex) {
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() throws SupplyException { throw ex; }
                    @Override
                    public Type getSuppliedType() { return Object.class; }
                    @Override
                    public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
                };
            }
            @Override
            public Type getSuppliedType() { return Object.class; }
            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
            @Override
            public boolean isContextual() { return false; }
        };
    }

    private static StatementNode stmt(Object value, String var) {
        return new StatementNode(constExpr(value), var, false, null, null, null, null);
    }

    // ---- StatementBlock ----

    @Test
    void statementBlockWithoutContextThrows() {
        StatementBlock block = new StatementBlock(List.of(stmt("x", null)));
        ScriptException ex = assertThrows(ScriptException.class, block::execute);
        assertTrue(ex.getMessage().contains("no runtime context"));
    }

    @Test
    void emptyStatementBlockReturnsNull() {
        StatementBlock block = new StatementBlock(null);
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> assertNull(block.execute()));
    }

    @Test
    void statementBlockReturnsLastStatementResult() {
        StatementBlock block = new StatementBlock(List.of(stmt("first", null), stmt("last", null)));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> assertEquals("last", block.execute()));
    }

    @Test
    void statementBlockStoresNamedVariableAndUnderscore() {
        StatementBlock block = new StatementBlock(List.of(stmt("hello", "greeting")));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            block.execute();
            assertEquals("hello", ctx.getVariable("greeting", IClass.getClass(String.class)).orElse(null));
            // The special "_" last-result variable is also set.
            assertEquals("hello", ctx.getVariable("_", IClass.getClass(String.class)).orElse(null));
        });
    }

    @Test
    void statementBlockOutputVariableRoutesToContextOutput() {
        StatementBlock block = new StatementBlock(List.of(stmt("the result", "output")));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            block.execute();
            assertEquals("the result", ctx.getOutput());
        });
    }

    @Test
    void statementBlockUncaughtScriptExceptionPropagates() {
        StatementNode failing = new StatementNode(
                throwing(new IllegalStateException("boom")), null, false, null, null, null, null);
        StatementBlock block = new StatementBlock(List.of(failing));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () ->
                assertThrows(ScriptException.class, block::execute));
    }

    @Test
    void statementBlockCatchClauseHandlesException() {
        // A statement that fails with IllegalStateException, with a catch-all handler
        // returning "recovered".
        CatchClause catchAll = new CatchClause(null, stmt("recovered", null));
        StatementNode failing = new StatementNode(
                throwing(new IllegalStateException("boom")), null, false, null,
                List.of(catchAll), null, null);
        StatementBlock block = new StatementBlock(List.of(failing));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () ->
                assertEquals("recovered", block.execute()));
    }

    // ---- ScriptFunction ----

    @Test
    void scriptFunctionArityMismatchThrows() {
        ScriptFunction fn = new ScriptFunction("add", List.of("a", "b"),
                new StatementBlock(List.of(stmt("x", null))));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            ScriptException ex = assertThrows(ScriptException.class, () -> fn.invoke("only-one"));
            assertTrue(ex.getMessage().contains("expects 2"));
            assertTrue(ex.getMessage().contains("got 1"));
        });
    }

    @Test
    void scriptFunctionWithoutContextThrows() {
        ScriptFunction fn = new ScriptFunction("f", List.of(),
                new StatementBlock(List.of(stmt("x", null))));
        // No bound context.
        ScriptException ex = assertThrows(ScriptException.class, fn::invoke);
        assertTrue(ex.getMessage().contains("No runtime context"));
    }

    @Test
    void scriptFunctionBindsParametersAndReturnsBodyResult() {
        // Body returns the value of parameter "p" (read via a fresh statement that
        // resolves the bound variable). We assert the function returns its body's
        // last statement result; here the body just yields a constant.
        ScriptFunction fn = new ScriptFunction("greet", List.of("p"),
                new StatementBlock(List.of(stmt("done", null))));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () ->
                assertEquals("done", fn.invoke("anything")));
    }

    @Test
    void scriptFunctionRestoresShadowedCallerVariable() {
        // Caller sets "p" = "caller". The function has a parameter "p" which shadows
        // it during the body, then the original value must be restored afterwards.
        ScriptFunction fn = new ScriptFunction("f", List.of("p"),
                new StatementBlock(List.of(stmt("body", null))));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            ctx.setVariable("p", "caller");
            fn.invoke("argument");
            // After invocation the caller's "p" is restored.
            assertEquals("caller", ctx.getVariable("p", IClass.getClass(String.class)).orElse(null));
        });
    }

    @Test
    void scriptFunctionToStringListsParameters() {
        ScriptFunction fn = new ScriptFunction("compute", List.of("x", "y"),
                new StatementBlock(List.of()));
        assertEquals("function compute(x, y)", fn.toString());
        assertEquals(List.of("x", "y"), fn.parameters());
    }

    // ---- CatchClauseHandler ----

    @Test
    void tryCatchClausesEmptyListReturnsNotCaught() {
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            var r = CatchClauseHandler.tryCatchClauses(ctx, List.of(),
                    new ScriptException("x"));
            assertEquals(false, r.caught());
            assertNull(r.handlerResult());
        });
    }

    @Test
    void tryCatchClausesNullListReturnsNotCaught() {
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            var r = CatchClauseHandler.tryCatchClauses(ctx, null, new ScriptException("x"));
            assertEquals(false, r.caught());
        });
    }

    @Test
    void tryCatchClausesMatchingUnwrapsCauseAndSetsContextVars() {
        // ScriptException wrapping an IllegalStateException("root cause"): the matcher
        // must test against the unwrapped cause, and "message"/"exception"/"code"
        // context variables must be populated.
        IllegalStateException root = new IllegalStateException("root cause");
        ScriptException wrapped = new ScriptException("outer", root);
        CatchClause cc = new CatchClause(List.of("java.lang.IllegalStateException"),
                stmt("handled", null), 503);
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            var r = CatchClauseHandler.tryCatchClauses(ctx, List.of(cc), wrapped);
            assertTrue(r.caught());
            assertEquals("handled", r.handlerResult());
            assertEquals(503, ctx.getCode().orElse(null));
            assertEquals("root cause",
                    ctx.getVariable("message", IClass.getClass(String.class)).orElse(null));
            assertSameCause(root, ctx);
        });
    }

    private static void assertSameCause(Throwable expected, IRuntimeContext<Object[], Object> ctx) {
        Object stored = ctx.getVariable("exception", IClass.getClass(Throwable.class)).orElse(null);
        assertEquals(expected, stored);
    }

    @Test
    void tryCatchClausesNonMatchingReturnsNotCaught() {
        ScriptException wrapped = new ScriptException("outer", new IllegalStateException("x"));
        CatchClause cc = new CatchClause(List.of("java.io.IOException"), stmt("nope", null));
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            var r = CatchClauseHandler.tryCatchClauses(ctx, List.of(cc), wrapped);
            assertEquals(false, r.caught());
        });
    }

    @Test
    void tryCatchClausesMatchWithoutHandlerStillSetsCode() {
        ScriptException ex = new ScriptException("direct"); // no cause -> matches against itself
        CatchClause cc = new CatchClause(null, null, 418); // catch-all, no handler, code 418
        IRuntimeContext<Object[], Object> ctx = startedContext();
        RuntimeExpressionContext.runIn(ctx, () -> {
            var r = CatchClauseHandler.tryCatchClauses(ctx, List.of(cc), ex);
            assertTrue(r.caught());
            assertNull(r.handlerResult());
            assertEquals(418, ctx.getCode().orElse(null));
        });
    }
}
