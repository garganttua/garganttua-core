package com.garganttua.core.runtime;

/**
 * Scoped accessor for the currently-executing {@link IRuntimeContext} during
 * expression evaluation.
 *
 * <p>Backed by a Java 21 {@link ScopedValue} (preview): the runtime context is
 * bound for the duration of a structured scope rather than imperatively pushed
 * and popped on a {@code ThreadLocal}. Callers use {@link #runIn} or
 * {@link #callIn} to establish the scope; nested runtime invocations create
 * nested scopes naturally — the outer binding is automatically restored when
 * the inner scope ends, so the propagation bugs that the previous push/pop
 * design needed to defend against (e.g., a sub-runtime clearing the holder
 * before the outer step's catch handler could read it) are eliminated by
 * construction.
 *
 * <p>Read sites use {@link #get()}, which returns {@code null} when no scope is
 * active (matching the previous {@code ThreadLocal.get()} semantics so existing
 * null-checks keep working).
 *
 * @since 2.0.0-ALPHA02
 */
public final class RuntimeExpressionContext {

    private static final ScopedValue<IRuntimeContext<?, ?>> CURRENT = ScopedValue.newInstance();

    private RuntimeExpressionContext() {
    }

    /**
     * @return the runtime context bound to the current scope, or {@code null}
     *         if no scope is active.
     */
    @SuppressWarnings("unchecked")
    public static <I, O> IRuntimeContext<I, O> get() {
        return CURRENT.isBound() ? (IRuntimeContext<I, O>) CURRENT.get() : null;
    }

    /**
     * Run {@code body} with {@code context} bound. The binding is unbound
     * automatically when {@code body} returns.
     */
    public static <X extends Throwable> void runIn(IRuntimeContext<?, ?> context, CtxRunnable<X> body) throws X {
        ScopedValue.where(CURRENT, context).run(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                throwUnchecked(t);
            }
        });
    }

    /**
     * Call {@code body} with {@code context} bound and return its result.
     */
    public static <R, X extends Exception> R callIn(IRuntimeContext<?, ?> context, CtxCallable<R, X> body) throws X {
        try {
            return ScopedValue.where(CURRENT, context).call(body::call);
        } catch (RuntimeException | Error re) {
            throw re;
        } catch (Exception e) {
            throwUnchecked(e);
            throw new IllegalStateException(e); // unreachable
        }
    }

    @SuppressWarnings("unchecked")
    private static <X extends Throwable> void throwUnchecked(Throwable t) throws X {
        throw (X) t;
    }

    /**
     * Runnable body that may throw a checked exception of type {@code X}.
     *
     * @param <X> the throwable type the body may raise
     */
    @FunctionalInterface
    public interface CtxRunnable<X extends Throwable> {
        /**
         * Runs the body.
         *
         * @throws X if the body fails
         */
        void run() throws X;
    }

    /**
     * Callable body returning {@code R} that may throw a checked exception of type {@code X}.
     *
     * @param <R> the result type
     * @param <X> the exception type the body may raise
     */
    @FunctionalInterface
    public interface CtxCallable<R, X extends Exception> {
        /**
         * Computes and returns the result.
         *
         * @return the computed value
         * @throws X if the body fails
         */
        R call() throws X;
    }
}
