package com.garganttua.core.expression;

import java.util.Optional;

import com.garganttua.core.supply.ISupplier;

public interface IExpression<R, S extends ISupplier<R>> extends ISupplier<S> {

    S evaluate() throws ExpressionException;

    @Override
    default Optional<S> supply() throws ExpressionException {
        return Optional.of(this.evaluate());
    }

}
