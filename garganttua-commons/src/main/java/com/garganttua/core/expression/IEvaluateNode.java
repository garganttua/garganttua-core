package com.garganttua.core.expression;

import com.garganttua.core.supply.ISupplier;

@FunctionalInterface
public interface IEvaluateNode<R> {

   ISupplier<R> evaluate(ISupplier<?> ...parameters) throws ExpressionException;
   
}
