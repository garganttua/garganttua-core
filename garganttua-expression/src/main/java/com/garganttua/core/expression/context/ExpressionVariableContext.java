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

    @FunctionalInterface
    public interface ResolverRunnable<X extends Throwable> {
        void run() throws X;
    }

    @FunctionalInterface
    public interface ResolverCallable<R, X extends Exception> {
        R call() throws X;
    }
}
