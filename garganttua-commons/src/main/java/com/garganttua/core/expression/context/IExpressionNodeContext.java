package com.garganttua.core.expression.context;

import java.util.List;

import com.garganttua.core.reflection.IClass;

/**
 * Carries the parameters and resolution metadata used by an
 * {@link IExpressionNodeFactory} to build an expression node.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IExpressionNodeContext {

    /**
     * Returns the parameters passed to the node factory. Each element is either an
     * {@link com.garganttua.core.expression.IExpressionNode} or a direct value.
     *
     * @return the ordered list of parameters
     */
    List<Object> parameters();

    /**
     * Indicates whether a contextual (context-aware) node should be built.
     *
     * @return {@code true} to build a contextual node, {@code false} for a plain node
     */
    boolean buildContextual();

    /**
     * Tests whether the given parameter types match this context's parameters.
     *
     * @param parameterTypes the candidate parameter types
     * @return {@code true} if the types match
     */
    boolean matches(IClass<?>[] parameterTypes);

    /**
     * Returns the resolved parameter types for this context.
     *
     * @return the parameter types
     */
    IClass<?>[] parameterTypes();

}
