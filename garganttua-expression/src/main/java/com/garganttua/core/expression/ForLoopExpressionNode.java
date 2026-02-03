package com.garganttua.core.expression;

import java.util.Optional;

import com.garganttua.core.expression.context.ExpressionVariableContext;
import com.garganttua.core.expression.context.IExpressionVariableResolver;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

public class ForLoopExpressionNode implements IExpressionNode<Object, ISupplier<Object>> {

    private static final int MAX_ITERATIONS = 10000;

    private final String variableName;
    private final IExpressionNode<?, ? extends ISupplier<?>> updateNode;
    private final IExpressionNode<?, ? extends ISupplier<?>> conditionNode;
    private final IExpressionNode<?, ? extends ISupplier<?>> bodyNode;

    public ForLoopExpressionNode(String variableName,
                                  IExpressionNode<?, ? extends ISupplier<?>> updateNode,
                                  IExpressionNode<?, ? extends ISupplier<?>> conditionNode,
                                  IExpressionNode<?, ? extends ISupplier<?>> bodyNode) {
        this.variableName = variableName;
        this.updateNode = updateNode;
        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
    }

    @Override
    public ISupplier<Object> evaluate() throws ExpressionException {
        return new ISupplier<Object>() {
            @Override
            public java.lang.reflect.Type getSuppliedType() {
                return Object.class;
            }
            @Override
            public Optional<Object> supply() throws SupplyException {
                try {
                    Object lastResult = null;
                    int iterations = 0;
                    while (iterations < MAX_ITERATIONS) {
                        ISupplier<?> condSupplier = conditionNode.evaluate();
                        Object condResult = condSupplier.supply().orElse(null);
                        if (!(condResult instanceof Boolean b && b)) {
                            break;
                        }
                        ISupplier<?> bodySupplier = bodyNode.evaluate();
                        lastResult = bodySupplier.supply().orElse(null);
                        ISupplier<?> updateSupplier = updateNode.evaluate();
                        Object updateResult = updateSupplier.supply().orElse(null);
                        setVariable(variableName, updateResult);
                        iterations++;
                    }
                    if (iterations >= MAX_ITERATIONS) {
                        throw new SupplyException("For loop exceeded maximum iterations (" + MAX_ITERATIONS + ")");
                    }
                    return Optional.ofNullable(lastResult);
                } catch (SupplyException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SupplyException("For loop execution failed", e);
                }
            }
        };
    }

    private static void setVariable(String name, Object value) {
        IExpressionVariableResolver resolver = ExpressionVariableContext.get();
        if (resolver instanceof VariableSettableResolver settable) {
            settable.setVariable(name, value);
        }
    }

    @Override
    public Optional<ISupplier<Object>> supply() throws SupplyException {
        return Optional.of(this.evaluate());
    }

    @Override
    public Class<Object> getFinalSuppliedClass() {
        return Object.class;
    }

    @Override
    public java.lang.reflect.Type getSuppliedType() {
        return Object.class;
    }

    public interface VariableSettableResolver extends IExpressionVariableResolver {
        void setVariable(String name, Object value);
    }
}
