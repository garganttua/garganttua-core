package com.garganttua.core.runtime;

/**
 * Thread-local holder for the current runtime context during expression evaluation.
 *
 * <p>
 * This class provides a mechanism for expressions to access the current
 * {@link IRuntimeContext} during evaluation. The runtime step binders set the
 * context before evaluating expressions and clear it afterwards.
 * </p>
 */
public final class RuntimeExpressionContext {

    private static final ThreadLocal<IRuntimeContext<?, ?>> CURRENT = new ThreadLocal<>();

    private RuntimeExpressionContext() {
    }

    public static void set(IRuntimeContext<?, ?> context) {
        CURRENT.set(context);
    }

    @SuppressWarnings("unchecked")
    public static <I, O> IRuntimeContext<I, O> get() {
        return (IRuntimeContext<I, O>) CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
