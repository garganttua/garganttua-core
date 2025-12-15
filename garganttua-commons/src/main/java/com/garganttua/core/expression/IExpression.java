package com.garganttua.core.expression;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

public interface IExpression<R, S extends ISupplier<R>> extends ISupplierBuilder<R, S> {

    S evaluate() throws ExpressionException;

    @Override
    default S build() throws DslException {
        return this.evaluate();
    }
}
