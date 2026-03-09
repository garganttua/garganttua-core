package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.supply.ISupplier;

public class StatementNode implements IScriptNode {

    private final IExpression<?, ? extends ISupplier<?>> expression;
    private final String variableName;
    private final boolean assignExpression;
    private final Integer code;
    private final List<CatchClause> catchClauses;
    private final List<CatchClause> downstreamCatchClauses;
    private final List<PipeClause> pipeClauses;
    private final int line;
    private final String sourceText;

    public StatementNode(IExpression<?, ? extends ISupplier<?>> expression, String variableName,
            boolean assignExpression, Integer code,
            List<CatchClause> catchClauses, List<CatchClause> downstreamCatchClauses,
            List<PipeClause> pipeClauses) {
        this(expression, variableName, assignExpression, code, catchClauses, downstreamCatchClauses, pipeClauses, 0, null);
    }

    public StatementNode(IExpression<?, ? extends ISupplier<?>> expression, String variableName,
            boolean assignExpression, Integer code,
            List<CatchClause> catchClauses, List<CatchClause> downstreamCatchClauses,
            List<PipeClause> pipeClauses, int line, String sourceText) {
        this.expression = expression;
        this.variableName = variableName;
        this.assignExpression = assignExpression;
        this.code = code;
        this.catchClauses = catchClauses != null ? catchClauses : List.of();
        this.downstreamCatchClauses = downstreamCatchClauses != null ? downstreamCatchClauses : List.of();
        this.pipeClauses = pipeClauses != null ? pipeClauses : List.of();
        this.line = line;
        this.sourceText = sourceText;
    }

    @Override
    public Object execute() throws ScriptException {
        try {
            ISupplier<?> supplier = this.expression.evaluate();
            return supplier.supply().orElse(null);
        } catch (Exception e) {
            String msg = "Expression execution failed";
            if (this.line > 0) {
                msg += " at line " + this.line;
            }
            if (this.sourceText != null) {
                msg += ": " + this.sourceText;
            }
            throw new ScriptException(msg, e);
        }
    }

    @Override
    public String variableName() {
        return this.variableName;
    }

    @Override
    public boolean assignExpression() {
        return this.assignExpression;
    }

    @Override
    public Integer code() {
        return this.code;
    }

    @Override
    public IExpression<?, ? extends ISupplier<?>> expression() {
        return this.expression;
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

    @Override
    public int line() {
        return this.line;
    }

    @Override
    public String sourceText() {
        return this.sourceText;
    }
}
