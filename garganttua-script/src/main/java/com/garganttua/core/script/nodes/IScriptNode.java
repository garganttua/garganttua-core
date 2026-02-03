package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.supply.ISupplier;

public interface IScriptNode {

    Object execute() throws ScriptException;

    String variableName();

    boolean assignExpression();

    Integer code();

    IExpression<?, ? extends ISupplier<?>> expression();

    List<CatchClause> catchClauses();

    List<CatchClause> downstreamCatchClauses();

    List<PipeClause> pipeClauses();
}
