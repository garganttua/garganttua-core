package com.garganttua.core.expression.context;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionContext {

    IExpression<?, ? extends ISupplier<?>> expression(String expression);

}
