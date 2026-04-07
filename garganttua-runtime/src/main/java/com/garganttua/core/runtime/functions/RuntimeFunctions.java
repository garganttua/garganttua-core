package com.garganttua.core.runtime.functions;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.RuntimeExpressionContext;

/**
 * Expression functions for accessing the runtime execution context.
 *
 * <p>
 * This class provides an {@code @Expression} annotated function that exposes the
 * current {@link IRuntimeContext} to the expression language. Since
 * {@code IRuntimeContext} extends {@code IInjectionContext}, this gives scripts
 * access to the local (child) injection context created by the runtime, rather
 * than the global singleton.
 * </p>
 *
 * <p><b>Usage in a .gs script</b></p>
 * <pre>{@code
 * ctx <- context()
 * bean <- :queryBean(ctx, cast(Optional.class), "myBeanRef")
 * }</pre>
 *
 * <p>
 * The {@link RuntimeExpressionContext} ThreadLocal is set by
 * {@code RuntimeStepMethodBinder} before any
 * expression evaluation, so {@code context()} is guaranteed to return a
 * non-null value when called during runtime execution.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IRuntimeContext
 * @see RuntimeExpressionContext
 */
public final class RuntimeFunctions {

    private RuntimeFunctions() {}

    /**
     * Returns the current runtime context for the executing thread.
     *
     * <p>
     * The returned {@link IRuntimeContext} is the local child injection context
     * created by the runtime for the current execution. It provides access to
     * beans, properties, and variables scoped to this runtime instance.
     * </p>
     *
     * @return the current runtime context, never {@code null}
     * @throws ExpressionException if no runtime context is available on the
     *         current thread (i.e. called outside of a runtime execution)
     */
    @Expression(name = "context", description = "Returns the current runtime context (local injection context)")
    public static IRuntimeContext<?, ?> runtimeContext() {
        IRuntimeContext<?, ?> ctx = RuntimeExpressionContext.get();
        if (ctx == null) {
            throw new ExpressionException(
                "No runtime context available. Ensure this is called within a runtime execution.");
        }
        return ctx;
    }
}
