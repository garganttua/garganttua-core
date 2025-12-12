package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;

public class ExpressionLeaf<R> implements IExpressionNode<R, ISupplier<R>>{

    private IEvaluateLeaf<R> evaluate;

    private Class<R> returnedType;

    private String name;

    private Object[] parameters;

    public ExpressionLeaf(String name, IEvaluateLeaf<R> evaluate, Class<R> returnedType, Object ...parameters) {
        this.returnedType = returnedType;
        this.parameters = Objects.requireNonNull(parameters, "Parameters cannot be null");;
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
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
        return this.evaluate.evaluate(this.parameters);
    }

    @Override
    public Class<R> getFinalSuppliedClass() {
        return this.returnedType;
    }

}
