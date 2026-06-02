package com.garganttua.core.expression.context;

/**
 * Scoped accessor for the currently-bound {@link IExpressionVariableResolver}.
 *
 * <p>Backed by a Java 21 {@link ScopedValue} (preview): the resolver is bound
 * for the duration of a structured scope rather than imperatively set/cleared
 * on a {@code ThreadLocal}. Callers use {@link #runIn} or {@link #callIn} to
 * establish the scope; nested re-binding is automatic (via nested
 * {@code ScopedValue.where(...)} calls).
 *
 * @since 2.0.0-ALPHA02
 */
public final class ExpressionVariableContext {

    private static final ScopedValue<IExpressionVariableResolver> RESOLVER = ScopedValue.newInstance();

    private ExpressionVariableContext() {
    }

    /**
     * @return the resolver bound to the current scope, or {@code null} if none
     *         is bound (matches the previous {@code ThreadLocal.get()} semantics).
     */
    public static IExpressionVariableResolver get() {
        return RESOLVER.isBound() ? RESOLVER.get() : null;
    }

    /**
     * Binds {@code resolver} to the current scope for the duration of {@code body} and runs it.
     *
     * @param <X>      the checked exception type the body may throw
     * @param resolver the resolver to bind for the scope
     * @param body     the action to run with the resolver bound
     * @throws X if the body throws
     */
    public static <X extends Throwable> void runIn(IExpressionVariableResolver resolver,
            ResolverRunnable<X> body) throws X {
        ScopedValue.where(RESOLVER, resolver).run(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                throwUnchecked(t);
            }
        });
    }

    /**
     * Binds {@code resolver} to the current scope, runs {@code body}, and returns its result.
     *
     * @param <R>      the result type
     * @param <X>      the checked exception type the body may throw
     * @param resolver the resolver to bind for the scope
     * @param body     the action to run with the resolver bound
     * @return the value produced by {@code body}
     * @throws X if the body throws
     */
    public static <R, X extends Exception> R callIn(IExpressionVariableResolver resolver,
            ResolverCallable<R, X> body) throws X {
        try {
            return ScopedValue.where(RESOLVER, resolver).call(body::call);
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
     * A runnable body executed within a bound resolver scope, allowed to throw a checked exception.
     *
     * @param <X> the checked exception type the body may throw
     */
    @FunctionalInterface
    public interface ResolverRunnable<X extends Throwable> {
        /**
         * Runs the body.
         *
         * @throws X if the body fails
         */
        void run() throws X;
    }

    /**
     * A value-returning body executed within a bound resolver scope, allowed to throw a checked exception.
     *
     * @param <R> the result type
     * @param <X> the checked exception type the body may throw
     */
    @FunctionalInterface
    public interface ResolverCallable<R, X extends Exception> {
        /**
         * Computes and returns the body's result.
         *
         * @return the computed value
         * @throws X if the body fails
         */
        R call() throws X;
    }
}
