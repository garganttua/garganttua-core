package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;

public class ContextualExpressionNode<R>
        implements IContextualExpressionNode<R, IContextualSupplier<R, IExpressionContext>> {

    private List<IExpressionNode<?, ? extends ISupplier<?>>> childs = new LinkedList<>();

    private IContextualEvaluate<R> evaluate;

    private Class<R> returnedType;

    private String name;

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, Class<R> returnedType) {
        this.returnedType = returnedType;
        this.childs = List.of();
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
    }

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, Class<R> returnedType,
            List<IExpressionNode<?, ? extends ISupplier<?>>> childs) {
        this.childs = Objects.requireNonNull(childs, "Childs list cannot be null");
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
        this.returnedType = returnedType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Type getSuppliedType() {
        Type raw = ObjectReflectionHelper
                .getParameterizedType(IContextualSupplier.class, this.returnedType, ExpressionContext.class)
                .getRawType();

        return (Class<IContextualSupplier<R, IExpressionContext>>) raw;
    }

    @Override
    public IContextualSupplier<R, IExpressionContext> evaluate(IExpressionContext ownerContext,
            Object... otherContexts) throws ExpressionException {

        List<ISupplier<?>> childsSignals = this.childs.stream()
                .map(node -> Expression.evaluateNode(node, ownerContext))
                .collect(Collectors.toList());

        return this.evaluate.evaluate(ownerContext, childsSignals.toArray(new ISupplier<?>[0]));
    }

    @Override
    public Class<R> getFinalSuppliedClass() {
        return this.returnedType;
    }

}
