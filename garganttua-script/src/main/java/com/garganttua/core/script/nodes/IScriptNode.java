package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.supply.ISupplier;

/**
 * A single node in the script AST: a statement, statement group, or function
 * definition. Carries the optional variable binding, exit code, exception
 * handling clauses, conditional pipes, and source location for the node.
 */
public interface IScriptNode {

    /**
     * Executes this node.
     *
     * @return the node's result, or {@code null}
     * @throws ScriptException if execution fails
     */
    Object execute() throws ScriptException;

    /** Returns the name of the variable this node assigns to, or {@code null}. */
    String variableName();

    /**
     * Returns {@code true} if the expression itself (the supplier) is assigned without
     * eager evaluation (the {@code =} operator), {@code false} if its result is assigned
     * (the {@code <-} operator).
     */
    boolean assignExpression();

    /** Returns the exit code associated with this node, or {@code null}. */
    Integer code();

    /** Returns the expression evaluated by this node, or {@code null} for non-expression nodes. */
    IExpression<?, ? extends ISupplier<?>> expression();

    /** Returns the immediate ({@code !}) catch clauses for this node. */
    List<CatchClause> catchClauses();

    /** Returns the downstream ({@code *}) catch clauses for this node. */
    List<CatchClause> downstreamCatchClauses();

    /** Returns the conditional pipe ({@code |}) clauses for this node. */
    List<PipeClause> pipeClauses();

    /** Returns the source line of this node. */
    int line();

    /** Returns the original source text of this node, or {@code null}. */
    String sourceText();
}
