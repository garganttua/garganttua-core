package com.garganttua.core.expression.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;

/**
 * Behaviour tests for the {@link ExpressionVariableContext} ScopedValue holder: bound/unbound
 * {@link ExpressionVariableContext#get()} semantics, scope confinement, nested re-binding,
 * value passthrough from {@code callIn}, and checked/unchecked exception propagation.
 */
public class ExpressionVariableContextBehaviourTest {

    /** A resolver returning {@code name} as its value, ignoring the type. */
    private IExpressionVariableResolver echo(String prefix) {
        return new IExpressionVariableResolver() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Optional<T> resolve(String name, IClass<T> type) {
                return Optional.of((T) (prefix + name));
            }
        };
    }

    @Test
    public void get_outsideAnyScope_returnsNull() {
        assertNull(ExpressionVariableContext.get(),
                "no resolver bound to the current (thread) scope must read back as null");
    }

    @Test
    public void runIn_bindsResolverForBodyDuration() throws Throwable {
        IExpressionVariableResolver resolver = echo("r:");
        AtomicReference<IExpressionVariableResolver> insideScope = new AtomicReference<>();

        ExpressionVariableContext.runIn(resolver, () -> insideScope.set(ExpressionVariableContext.get()));

        assertSame(resolver, insideScope.get(), "resolver must be visible inside the scope");
        assertNull(ExpressionVariableContext.get(), "resolver must be cleared once the scope exits");
    }

    @Test
    public void runIn_propagatesCheckedException() {
        IExpressionVariableResolver resolver = echo("r:");
        IOException ex = assertThrows(IOException.class,
                () -> ExpressionVariableContext.runIn(resolver, () -> {
                    throw new IOException("checked");
                }));
        assertEquals("checked", ex.getMessage());
        // scope must still be cleaned up even after a throwing body
        assertNull(ExpressionVariableContext.get());
    }

    @Test
    public void runIn_propagatesUncheckedException() {
        IExpressionVariableResolver resolver = echo("r:");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ExpressionVariableContext.runIn(resolver, () -> {
                    throw new IllegalStateException("unchecked");
                }));
        assertEquals("unchecked", ex.getMessage());
    }

    @Test
    public void callIn_returnsBodyResult() throws Exception {
        IExpressionVariableResolver resolver = echo("r:");
        String result = ExpressionVariableContext.callIn(resolver,
                () -> ExpressionVariableContext.get().resolve("X", IClass.getClass(String.class)).get());
        assertEquals("r:X", result);
        assertNull(ExpressionVariableContext.get(), "resolver cleared after callIn returns");
    }

    @Test
    public void callIn_propagatesCheckedException() {
        IExpressionVariableResolver resolver = echo("r:");
        IOException ex = assertThrows(IOException.class,
                () -> ExpressionVariableContext.callIn(resolver, () -> {
                    throw new IOException("boom-call");
                }));
        assertEquals("boom-call", ex.getMessage());
    }

    @Test
    public void callIn_propagatesUncheckedExceptionUnchanged() {
        IExpressionVariableResolver resolver = echo("r:");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ExpressionVariableContext.callIn(resolver, () -> {
                    throw new RuntimeException("rt");
                }));
        assertEquals("rt", ex.getMessage());
    }

    @Test
    public void nestedScopes_innerOverridesThenRestoresOuter() throws Throwable {
        IExpressionVariableResolver outer = echo("outer:");
        IExpressionVariableResolver inner = echo("inner:");
        AtomicReference<IExpressionVariableResolver> beforeInner = new AtomicReference<>();
        AtomicReference<IExpressionVariableResolver> duringInner = new AtomicReference<>();
        AtomicReference<IExpressionVariableResolver> afterInner = new AtomicReference<>();

        ExpressionVariableContext.runIn(outer, () -> {
            beforeInner.set(ExpressionVariableContext.get());
            ExpressionVariableContext.runIn(inner, () -> duringInner.set(ExpressionVariableContext.get()));
            afterInner.set(ExpressionVariableContext.get());
        });

        assertSame(outer, beforeInner.get());
        assertSame(inner, duringInner.get(), "inner scope must shadow the outer resolver");
        assertSame(outer, afterInner.get(), "outer resolver must be restored after inner scope closes");
        assertNull(ExpressionVariableContext.get());
    }

    @Test
    public void callIn_resolverReadsValueOfExpectedType() throws Exception {
        IExpressionVariableResolver resolver = echo("v:");
        boolean present = ExpressionVariableContext.callIn(resolver,
                () -> ExpressionVariableContext.get().resolve("k", IClass.getClass(String.class)).isPresent());
        assertTrue(present);
    }
}
