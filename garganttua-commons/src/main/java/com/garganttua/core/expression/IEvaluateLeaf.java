package com.garganttua.core.expression;

import com.garganttua.core.supply.ISupplier;

@FunctionalInterface
public interface IEvaluateLeaf<R> {

    ISupplier<R> evaluate(Object ...parameters) throws ExpressionException;

}
