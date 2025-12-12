package com.garganttua.core.expression.context;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

public class ExpressionContext implements IExpressionContext {

    private Map<String, IExpressionNodeFactory<?,? extends ISupplier<?>>> nodeFactories;

    public ExpressionContext(Set<IExpressionNodeFactory<?,? extends ISupplier<?>>> nodeFactories) {
        Objects.requireNonNull(nodeFactories, "Node Factories set cannot be null");
        this.nodeFactories = nodeFactories.stream().collect(Collectors.toMap(IExpressionNodeFactory::key, ef -> ef));
    }

    @Override
    public IExpression<?, ? extends ISupplier<?>> expression(String expression) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'expression'");
    }

}
