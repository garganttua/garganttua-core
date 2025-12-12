package com.garganttua.core.expression;

import java.util.Optional;

import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

public interface IExpressionNode<R, S extends ISupplier<R>> extends ISupplier<S> {

    S evaluate() throws ExpressionException;

    @Override
    default Optional<S> supply() throws SupplyException {
        return Optional.of(this.evaluate());
    }

    Class<R> getFinalSuppliedClass();

}
