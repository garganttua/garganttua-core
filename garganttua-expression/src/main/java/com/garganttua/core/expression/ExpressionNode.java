package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;

public class ExpressionNode<R> implements IExpressionNode<R, ISupplier<R>> {

    private List<Object> params = new LinkedList<>();

    private IEvaluateNode<R> evaluate;

    private Class<R> returnedType;

    private String name;

    public ExpressionNode(String name, IEvaluateNode<R> evaluate, Class<R> returnedType) {
        this.returnedType = returnedType;
        this.params = List.of();
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
    }

    public ExpressionNode(String name, IEvaluateNode<R> evaluate, Class<R> returnedType,
            List<Object> params) {
        this.params = Objects.requireNonNull(params, "Params list cannot be null");
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
        List<Object> childs = this.params.stream()
                .map(p -> {
                    if (p instanceof IExpressionNode<?, ? extends ISupplier<?>> node)
                        return (Object) Expression.evaluateNode(node);
                    else 
                        return p;
                })
                .collect(Collectors.toList());

        Object[] params = childs.toArray(new Object[0]);
        return this.evaluate.evaluate(params);
    }

    @Override
    public Class<R> getFinalSuppliedClass() {
        return this.returnedType;
    }

}
