package com.garganttua.core.expression;

import java.util.Optional;

import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public interface IExpressionMethodBinder<T> extends IContextualMethodBinder<T, IExpressionContext>{

    String expressionName();

    Optional<T> evaluate(IExpressionContext expressionContext) throws ExpressionException;

    @Override
    default Optional<T> supply(IExpressionContext expressionContext, Object... otherContexts) throws ExpressionException {
        return this.evaluate(expressionContext);
    }

}
