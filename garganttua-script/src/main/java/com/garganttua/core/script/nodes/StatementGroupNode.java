package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.supply.ISupplier;

/**
 * Represents a group of statements enclosed in parentheses.
 * The group can have its own code mapping, catch clauses, and pipe clauses
 * that apply to the entire group.
 *
 * <pre>
 * (
 *   print("coucou") -&gt; 20
 *   data &lt;- "bonjour" -&gt; 21
 * ) -&gt; 60
 * | condition =&gt; handler
 * ! =&gt; catchHandler
 * </pre>
 */
public class StatementGroupNode implements IScriptNode {

    private final List<IScriptNode> statements;
    private final String variableName;
    private final Integer code;
    private final List<CatchClause> catchClauses;
    private final List<CatchClause> downstreamCatchClauses;
    private final List<PipeClause> pipeClauses;

    public StatementGroupNode(List<IScriptNode> statements, String variableName, Integer code,
            List<CatchClause> catchClauses, List<CatchClause> downstreamCatchClauses,
            List<PipeClause> pipeClauses) {
        this.statements = statements != null ? List.copyOf(statements) : List.of();
        this.variableName = variableName;
        this.code = code;
        this.catchClauses = catchClauses != null ? catchClauses : List.of();
        this.downstreamCatchClauses = downstreamCatchClauses != null ? downstreamCatchClauses : List.of();
        this.pipeClauses = pipeClauses != null ? pipeClauses : List.of();
    }

    /**
     * Returns the list of statements in this group.
     */
    public List<IScriptNode> statements() {
        return this.statements;
    }

    @Override
    public Object execute() throws ScriptException {
        // Execution is delegated - this method is not called directly
        // The ScriptContext executor handles group execution
        throw new UnsupportedOperationException("Group execution is handled by the executor");
    }

    @Override
    public String variableName() {
        return this.variableName;
    }

    @Override
    public boolean assignExpression() {
        return false; // Groups always assign result (like result assignment)
    }

    @Override
    public Integer code() {
        return this.code;
    }

    @Override
    public IExpression<?, ? extends ISupplier<?>> expression() {
        return null; // Groups don't have a single expression
    }

    @Override
    public List<CatchClause> catchClauses() {
        return this.catchClauses;
    }

    @Override
    public List<CatchClause> downstreamCatchClauses() {
        return this.downstreamCatchClauses;
    }

    @Override
    public List<PipeClause> pipeClauses() {
        return this.pipeClauses;
    }

    /**
     * Checks if this is a statement group.
     */
    public boolean isGroup() {
        return true;
    }
}
