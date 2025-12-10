package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;

public class ExpressionNode<R> implements IExpressionNode<R, ISupplier<R>> {

    private List<IExpressionNode<?, ? extends ISupplier<?>>> childs = new LinkedList<>();

    private IEvaluateNode<R> evaluate;

    private Class<R> returnedType;

    private String name;

    public ExpressionNode(String name, IEvaluateNode<R> evaluate, Class<R> returnedType) {
        this.returnedType = returnedType;
        this.childs = List.of();
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
    }

    public ExpressionNode(String name, IEvaluateNode<R> evaluate,
            List<IExpressionNode<?, ? extends ISupplier<?>>> childs, Class<R> returnedType) {
        this.childs = Objects.requireNonNull(childs, "Childs list cannot be null");
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
        this.returnedType = returnedType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Type getSuppliedType() {
        Type raw = ObjectReflectionHelper
                .getParameterizedType(ISupplier.class, this.returnedType)
                .getRawType();
        return (Class<ISupplier<R>>) raw;
    }

    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        List<ISupplier<?>> childsSignals = this.childs.stream()
                .map(node -> Expression.evaluateNode(node))
                .collect(Collectors.toList());

        return this.evaluate.evaluate(childsSignals.toArray(new ISupplier<?>[0]));
    }
}
