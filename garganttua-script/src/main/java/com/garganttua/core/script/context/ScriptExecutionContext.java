package com.garganttua.core.script.context;

public class ScriptExecutionContext {

    private static final ThreadLocal<ScriptContext> CURRENT = new ThreadLocal<>();

    public static void set(ScriptContext ctx) {
        CURRENT.set(ctx);
    }

    public static ScriptContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
