package com.garganttua.core.script.context;

/**
 * Scoped accessor for the currently-executing {@link ScriptContext}.
 *
 * <p>Backed by a Java 21 {@link ScopedValue} (preview): the context is bound for
 * the duration of a structured scope rather than imperatively pushed/popped on a
 * {@code ThreadLocal}. Callers use {@link #runIn(ScriptContext, ScriptRunnable)}
 * or {@link #callIn(ScriptContext, ScriptCallable)} to establish the scope.
 *
 * <p>Read sites use {@link #get()} which returns {@code null} when no scope is
 * active (matching the previous {@code ThreadLocal.get()} semantics).
 *
 * @since 2.0.0-ALPHA02
 */
public final class ScriptExecutionContext {

    private static final ScopedValue<ScriptContext> CURRENT = ScopedValue.newInstance();

    private ScriptExecutionContext() {
    }

    /**
     * @return the {@link ScriptContext} bound to the current scope, or
     *         {@code null} if no scope is active.
     */
    public static ScriptContext get() {
        return CURRENT.isBound() ? CURRENT.get() : null;
    }

    /**
     * Run {@code body} with {@code ctx} bound as the current script context.
     * The binding is removed automatically when {@code body} returns.
     */
    public static <X extends Throwable> void runIn(ScriptContext ctx, ScriptRunnable<X> body) throws X {
        ScopedValue.where(CURRENT, ctx).run(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                throwUnchecked(t);
            }
        });
    }

    /**
     * Call {@code body} with {@code ctx} bound as the current script context and
     * return its result.
     */
    public static <R, X extends Exception> R callIn(ScriptContext ctx, ScriptCallable<R, X> body) throws X {
        try {
            return ScopedValue.where(CURRENT, ctx).call(body::call);
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
     * Body executed by {@link #runIn(ScriptContext, ScriptRunnable)} with a
     * bound script context, allowed to throw a single checked type {@code X}.
     *
     * @param <X> the checked exception type the body may throw
     */
    @FunctionalInterface
    public interface ScriptRunnable<X extends Throwable> {
        /** @throws X if the body fails */
        void run() throws X;
    }

    /**
     * Result-producing body executed by
     * {@link #callIn(ScriptContext, ScriptCallable)} with a bound script
     * context, allowed to throw a single checked type {@code X}.
     *
     * @param <R> the result type
     * @param <X> the checked exception type the body may throw
     */
    @FunctionalInterface
    public interface ScriptCallable<R, X extends Exception> {
        /**
         * @return the computed result
         * @throws X if the body fails
         */
        R call() throws X;
    }
}
