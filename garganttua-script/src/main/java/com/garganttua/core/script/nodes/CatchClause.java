package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.reflection.IClass;

/**
 * A catch clause in the script AST, matching one or more exception types and
 * optionally running a handler node and/or setting an exit code when matched.
 *
 * <p>An empty or {@code null} exception type list is treated as catch-all.
 */
public class CatchClause {

    private final List<String> exceptionTypes;
    private final IScriptNode handler;
    private final Integer code;

    /**
     * Creates a catch clause with no associated exit code.
     *
     * @param exceptionTypes the exception type names to match (empty/null = catch-all)
     * @param handler        the node to execute when matched, or {@code null}
     */
    public CatchClause(List<String> exceptionTypes, IScriptNode handler) {
        this(exceptionTypes, handler, null);
    }

    /**
     * Creates a catch clause.
     *
     * @param exceptionTypes the exception type names to match (empty/null = catch-all)
     * @param handler        the node to execute when matched, or {@code null}
     * @param code           the exit code to apply when matched, or {@code null}
     */
    public CatchClause(List<String> exceptionTypes, IScriptNode handler, Integer code) {
        this.exceptionTypes = exceptionTypes;
        this.handler = handler;
        this.code = code;
    }

    /** Returns the exception type names this clause matches. */
    public List<String> exceptionTypes() {
        return this.exceptionTypes;
    }

    /** Returns the handler node to run when matched, or {@code null}. */
    public IScriptNode handler() {
        return this.handler;
    }

    /** Returns the exit code applied when matched, or {@code null}. */
    public Integer code() {
        return this.code;
    }

    /** Returns {@code true} if this clause matches any exception (no declared types). */
    public boolean isCatchAll() {
        return this.exceptionTypes == null || this.exceptionTypes.isEmpty();
    }

    /**
     * Tests whether the given throwable matches this clause. Resolves each declared
     * type via {@link IClass#forName(String)}; if the type cannot be loaded, falls
     * back to matching by simple or fully-qualified name suffix.
     *
     * @param t the throwable to test
     * @return {@code true} if this clause catches {@code t}
     */
    public boolean matches(Throwable t) {
        if (this.isCatchAll()) {
            return true;
        }
        for (String type : this.exceptionTypes) {
            try {
                IClass<?> clazz = IClass.forName(type);
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
