package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;

public class ContextualExpressionNode<R>
        implements IContextualExpressionNode<R, IContextualSupplier<R, IExpressionContext>> {

    private List<Object> params = new LinkedList<>();

    private IContextualEvaluate<R> evaluate;

    private volatile IClass<R> returnedType;

    private String name;

    /**
     * Indicates which parameters should be passed as lazy (unevaluated) ISupplier.
     */
    private List<Boolean> lazyParameters;

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, IClass<R> returnedType) {
        this.returnedType = returnedType;
        this.params = List.of();
        this.lazyParameters = List.of();
        this.evaluate = Objects.requireNonNull(evaluate, "Evaluate function cannot be null");
    }

    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, IClass<R> returnedType,
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
    public ContextualExpressionNode(String name, IContextualEvaluate<R> evaluate, IClass<R> returnedType,
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

    @Override
    public Type getSuppliedType() {
        return (Class<IContextualSupplier<R, IExpressionContext>>) (Class<?>) IContextualSupplier.class;
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
        IContextualSupplier<R, IExpressionContext> supplier = this.evaluate.evaluate(ownerContext, params);

        // For generic methods (returnedType is Object), wrap the supplier
        // to dynamically resolve the actual return type from the result value
        if (this.returnedType.getType() == Object.class) {
            final ContextualExpressionNode<R> self = this;
            IContextualSupplier<R, IExpressionContext> original = supplier;
            return new IContextualSupplier<R, IExpressionContext>() {
                @Override
                public java.util.Optional<R> supply(IExpressionContext context, Object... otherCtxs)
                        throws com.garganttua.core.supply.SupplyException {
                    java.util.Optional<R> result = original.supply(context, otherCtxs);
                    result.ifPresent(value -> {
                        if (value != null && self.returnedType.getType() == Object.class) {
                            self.returnedType = (IClass<R>) IClass.getClass(value.getClass());
                        }
                    });
                    return result;
                }

                @Override
                public IClass<IExpressionContext> getOwnerContextType() {
                    return original.getOwnerContextType();
                }

                @Override
                public java.lang.reflect.Type getSuppliedType() {
                    return original.getSuppliedType();
                }

                @Override
                public IClass<R> getSuppliedClass() {
                    return original.getSuppliedClass();
                }
            };
        }

        return supplier;
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
                return node.getFinalSuppliedClass().getType();
            }

            @Override
            public IClass<Object> getSuppliedClass() {
                return (IClass<Object>) (IClass<?>) node.getFinalSuppliedClass();
            }
        };
    }

    @Override
    public IClass<R> getFinalSuppliedClass() {
        return this.returnedType;
    }

}
