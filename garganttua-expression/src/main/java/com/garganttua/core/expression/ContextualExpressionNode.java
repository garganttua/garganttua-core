package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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

    /**
     * Indicates which parameters should be passed as lazy (unevaluated) ISupplier.
     */
    private List<Boolean> lazyParameters;

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, Class<R> returnedType) {
        this.returnedType = returnedType;
        this.params = List.of();
        this.lazyParameters = List.of();
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
    }

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, Class<R> returnedType,
            List<Object> params) {
        this(name, evaluate, returnedType, params, null);
    }

    /**
     * Creates a contextual expression node with lazy parameter support.
     *
     * @param name the node name
     * @param evaluate the evaluation function
     * @param returnedType the return type
     * @param params the parameters
     * @param lazyParameters list indicating which parameters are lazy (true = don't evaluate, pass as ISupplier)
     */
    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, Class<R> returnedType,
            List<Object> params, List<Boolean> lazyParameters) {
        this.params = Objects.requireNonNull(params, "Childs list cannot be null");
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

        List<Object> childs = new ArrayList<>(this.params.size());

        for (int i = 0; i < this.params.size(); i++) {
            Object p = this.params.get(i);
            boolean isLazy = i < lazyParameters.size() && Boolean.TRUE.equals(lazyParameters.get(i));

            if (p instanceof IExpressionNode<?, ? extends ISupplier<?>> node) {
                if (isLazy) {
                    // For lazy parameters, wrap the node in a supplier that evaluates on demand
                    childs.add(createLazySupplier(node, ownerContext));
                } else {
                    // Eager evaluation: evaluate the node now
                    childs.add(Expression.evaluateNode(node, ownerContext));
                }
            } else {
                childs.add(p);
            }
        }

        Object[] params = childs.toArray(new Object[0]);
        return this.evaluate.evaluate(ownerContext, params);

    }

    /**
     * Creates a lazy supplier that wraps an expression node.
     * The node will be evaluated when the supplier's supply() method is called.
     */
    private ISupplier<?> createLazySupplier(IExpressionNode<?, ? extends ISupplier<?>> node,
            IExpressionContext ownerContext) {
        return new ISupplier<Object>() {
            @Override
            public java.util.Optional<Object> supply() throws com.garganttua.core.supply.SupplyException {
                try {
                    ISupplier<?> evaluatedSupplier = Expression.evaluateNode(node, ownerContext);
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
