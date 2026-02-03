package com.garganttua.core.script.nodes;

import java.util.List;

public class CatchClause {

    private final List<String> exceptionTypes;
    private final IScriptNode handler;

    public CatchClause(List<String> exceptionTypes, IScriptNode handler) {
        this.exceptionTypes = exceptionTypes;
        this.handler = handler;
    }

    public List<String> exceptionTypes() {
        return this.exceptionTypes;
    }

    public IScriptNode handler() {
        return this.handler;
    }

    public boolean isCatchAll() {
        return this.exceptionTypes == null || this.exceptionTypes.isEmpty();
    }

    public boolean matches(Throwable t) {
        if (this.isCatchAll()) {
            return true;
        }
        for (String type : this.exceptionTypes) {
            try {
                Class<?> clazz = Class.forName(type);
                if (clazz.isInstance(t)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                if (t.getClass().getSimpleName().equals(type) || t.getClass().getName().endsWith("." + type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
