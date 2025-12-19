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

    private List<Object> params = new LinkedList<>();

    private IContextualEvaluate<R> evaluate;

    private Class<R> returnedType;

    private String name;

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, Class<R> returnedType) {
        this.returnedType = returnedType;
        this.params = List.of();
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
    }

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, Class<R> returnedType,
            List<Object> params) {
        this.params = Objects.requireNonNull(params, "Childs list cannot be null");
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

        List<Object> childs = this.params.stream()
                .map(p -> {
                    if (p instanceof IExpressionNode<?, ? extends ISupplier<?>> node)
                        return (Object) Expression.evaluateNode(node, ownerContext);
                    else 
                        return p;
                })
                .collect(Collectors.toList());

        Object[] params = childs.toArray(new Object[0]);
        return this.evaluate.evaluate(ownerContext, params);

    }

    @Override
    public Class<R> getFinalSuppliedClass() {
        return this.returnedType;
    }

}
