package com.garganttua.core.expression;

import java.util.Optional;

import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.SupplyException;

public interface IContextualExpressionNode<R, S extends IContextualSupplier<R, IExpressionContext>>
        extends IExpressionNode<R, S>, IContextualSupplier<S, IExpressionContext> {

    S evaluate(IExpressionContext ownerContext,
            Object... otherContexts) throws ExpressionException;

    @Override
    default Optional<S> supply(IExpressionContext ownerContext, Object... otherContexts) throws SupplyException {
        return Optional.of(this.evaluate(ownerContext, otherContexts));
    }

    @Override
    default S evaluate() throws ExpressionException {
        throw new ExpressionException(
                "Owner context of type " + getOwnerContextType().getSimpleName() + " required for this expression");
    }

    @Override
    default Optional<S> supply() throws ExpressionException {
        return this.supply();
    }

    @Override
        default Class<IExpressionContext> getOwnerContextType() {
        return IExpressionContext.class;
        }

}
