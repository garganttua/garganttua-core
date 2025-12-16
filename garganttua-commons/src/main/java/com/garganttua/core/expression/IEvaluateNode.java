package com.garganttua.core.expression;

import com.garganttua.core.supply.ISupplier;

/**
 * Functional interface for evaluating expression nodes with supplier parameters.
 *
 * <p>An expression node is a non-terminal element in an expression tree that takes
 * one or more supplier parameters and produces a result supplier. Expression nodes
 * represent functions or operations in the expression language.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an addition node
 * IEvaluateNode<Integer> addNode = (parameters) -> {
 *     Integer a = (Integer) parameters[0].supply().get();
 *     Integer b = (Integer) parameters[1].supply().get();
 *     return () -> Optional.of(a + b);
 * };
 *
 * // Evaluate the node
 * ISupplier<Integer> param1 = () -> Optional.of(10);
 * ISupplier<Integer> param2 = () -> Optional.of(20);
 * ISupplier<Integer> result = addNode.evaluate(param1, param2);
 * assertEquals(30, result.supply().get());
 * }</pre>
 *
 * @param <R> the type of value produced by this expression node
 * @see IEvaluateLeaf
 * @see IExpressionNode
 * @since 2.0.0-ALPHA01
 */
@FunctionalInterface
public interface IEvaluateNode<R> {

   /**
    * Evaluates this expression node with the given supplier parameters.
    *
    * @param parameters the supplier parameters to this node (lazily evaluated)
    * @return a supplier that provides the evaluation result
    * @throws ExpressionException if evaluation fails
    */
   ISupplier<R> evaluate(ISupplier<?> ...parameters) throws ExpressionException;

}
