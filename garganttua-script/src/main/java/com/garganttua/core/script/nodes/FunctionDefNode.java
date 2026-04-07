package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.supply.ISupplier;

/**
 * Represents a user-defined function definition in the script AST.
 *
 * <p>Syntax: {@code myFunc = (param1, param2) => (body)}
 *
 * <p>This node is not directly executable — it is compiled by
 * {@link com.garganttua.core.script.context.ScriptStepFactory}
 * into a RuntimeStep that creates a {@link ScriptFunction} and stores it
 * as a variable in the runtime context. The function can then be invoked via
 * {@link com.garganttua.core.expression.DynamicFunctionNode} dynamic resolution.
 *
 * <p>The {@code bodyBlockName} references a pre-compiled {@link StatementBlock}
 * extracted by {@link com.garganttua.core.script.context.BlockExpressionPreprocessor}.
 */
public class FunctionDefNode implements IScriptNode {

    private final String functionName;
    private final List<String> parameterNames;
    private final String bodyBlockName;
    private final int line;
    private final String sourceText;

    public FunctionDefNode(String functionName, List<String> parameterNames,
                           String bodyBlockName, int line, String sourceText) {
        this.functionName = functionName;
        this.parameterNames = List.copyOf(parameterNames);
        this.bodyBlockName = bodyBlockName;
        this.line = line;
        this.sourceText = sourceText;
    }

    public List<String> parameterNames() {
        return this.parameterNames;
    }

    public String bodyBlockName() {
        return this.bodyBlockName;
    }

    @Override
    public Object execute() throws ScriptException {
        throw new UnsupportedOperationException("Function definition is handled by the executor");
    }

    @Override
    public String variableName() {
        return this.functionName;
    }

    @Override
    public boolean assignExpression() {
        return false;
    }

    @Override
    public Integer code() {
        return null;
    }

    @Override
    public IExpression<?, ? extends ISupplier<?>> expression() {
        return null;
    }

    @Override
    public List<CatchClause> catchClauses() {
        return List.of();
    }

    @Override
    public List<CatchClause> downstreamCatchClauses() {
        return List.of();
    }

    @Override
    public List<PipeClause> pipeClauses() {
        return List.of();
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
