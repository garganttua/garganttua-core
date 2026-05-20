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

    /**
     * Push a context onto the thread-local stack, returning the previous value so
     * the caller can restore it via {@link #pop(IRuntimeContext)} in a {@code finally}.
     *
     * <p>
     * Use this pattern when a binder may be invoked in a nested fashion (e.g., a
     * nested step triggered from within a parent step's expression evaluation).
     * A naive {@code set()} / {@code clear()} pair nukes the outer context;
     * push/pop preserves it.
     * </p>
     */
    public static IRuntimeContext<?, ?> push(IRuntimeContext<?, ?> context) {
        IRuntimeContext<?, ?> previous = CURRENT.get();
        CURRENT.set(context);
        return previous;
    }

    /**
     * Restore the context returned by a prior {@link #push(IRuntimeContext)} call.
     * If {@code previous} is {@code null}, the thread-local is cleared.
     */
    public static void pop(IRuntimeContext<?, ?> previous) {
        if (previous != null) {
            CURRENT.set(previous);
        } else {
            CURRENT.remove();
        }
    }
}
