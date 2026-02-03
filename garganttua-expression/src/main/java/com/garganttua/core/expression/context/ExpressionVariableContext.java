package com.garganttua.core.expression.context;

public class ExpressionVariableContext {

    private static final ThreadLocal<IExpressionVariableResolver> RESOLVER = new ThreadLocal<>();

    public static void set(IExpressionVariableResolver resolver) {
        RESOLVER.set(resolver);
    }

    public static IExpressionVariableResolver get() {
        return RESOLVER.get();
    }

    public static void clear() {
        RESOLVER.remove();
    }
}
