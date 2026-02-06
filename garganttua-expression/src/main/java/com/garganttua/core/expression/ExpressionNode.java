package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;

public class ExpressionNode<R> implements IExpressionNode<R, ISupplier<R>> {

    private List<Object> params = new LinkedList<>();

    private IEvaluateNode<R> evaluate;

    private Class<R> returnedType;

    private String name;

    /**
     * Indicates which parameters should be passed as lazy (unevaluated) ISupplier.
     * If a parameter at index i has lazyParameters[i] = true, the IExpressionNode
     * is passed as-is (wrapped in a supplier) instead of being evaluated.
     */
    private List<Boolean> lazyParameters;

    public ExpressionNode(String name, IEvaluateNode<R> evaluate, Class<R> returnedType) {
        this.returnedType = returnedType;
        this.params = List.of();
        this.lazyParameters = List.of();
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
    }

    public ExpressionNode(String name, IEvaluateNode<R> evaluate, Class<R> returnedType,
            List<Object> params) {
        this(name, evaluate, returnedType, params, null);
    }

    /**
     * Creates an expression node with lazy parameter support.
     *
     * @param name the node name
     * @param evaluate the evaluation function
     * @param returnedType the return type
     * @param params the parameters
     * @param lazyParameters list indicating which parameters are lazy (true = don't evaluate, pass as ISupplier)
     */
    public ExpressionNode(String name, IEvaluateNode<R> evaluate, Class<R> returnedType,
            List<Object> params, List<Boolean> lazyParameters) {
        this.params = Objects.requireNonNull(params, "Params list cannot be null");
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
        this.returnedType = returnedType;
        this.lazyParameters = lazyParameters != null ? lazyParameters : createDefaultLazyParams(params.size());
    }

    private List<Boolean> createDefaultLazyParams(int size) {
        List<Boolean> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(Boolean.FALSE);
        }
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return ObjectReflectionHelper
                .getParameterizedType(ISupplier.class, this.returnedType)
                .getRawType();
    }

    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        List<Object> childs = new ArrayList<>(this.params.size());

        for (int i = 0; i < this.params.size(); i++) {
            Object p = this.params.get(i);
            boolean isLazy = i < lazyParameters.size() && Boolean.TRUE.equals(lazyParameters.get(i));

            if (p instanceof IExpressionNode<?, ? extends ISupplier<?>> node) {
                if (isLazy) {
                    // For lazy parameters, wrap the node in a supplier that evaluates on demand
                    childs.add(createLazySupplier(node));
                } else {
                    // Eager evaluation: evaluate the node now
                    childs.add(Expression.evaluateNode(node));
                }
            } else {
                childs.add(p);
            }
        }

        Object[] params = childs.toArray(new Object[0]);
        return this.evaluate.evaluate(params);
    }

    /**
     * Creates a lazy supplier that wraps an expression node.
     * The node will be evaluated when the supplier's supply() method is called.
     */
    private ISupplier<?> createLazySupplier(IExpressionNode<?, ? extends ISupplier<?>> node) {
        return new ISupplier<Object>() {
            @Override
            public java.util.Optional<Object> supply() throws com.garganttua.core.supply.SupplyException {
                try {
                    ISupplier<?> evaluatedSupplier = Expression.evaluateNode(node);
                    return evaluatedSupplier.supply().map(r -> (Object) r);
                } catch (ExpressionException e) {
                    throw new com.garganttua.core.supply.SupplyException("Lazy evaluation failed", e);
                }
            }

            @Override
            public Type getSuppliedType() {
                return node.getFinalSuppliedClass();
            }
        };
    }

    @Override
    public Class<R> getFinalSuppliedClass() {
        return this.returnedType;
    }

}
