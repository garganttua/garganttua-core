package com.garganttua.core.expression;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;

public interface IContextualEvaluate<R> {

    IContextualSupplier<R, IExpressionContext> evaluate(IExpressionContext context, ISupplier<?> ...parameters) throws ExpressionException;

}
