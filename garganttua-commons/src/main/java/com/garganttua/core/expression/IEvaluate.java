package com.garganttua.core.expression;

import com.garganttua.core.supply.ISupplier;

@FunctionalInterface
public interface IEvaluate<R> {

   ISupplier<R> evaluate(ISupplier<?> ...parameters) throws ExpressionException;
   
}
