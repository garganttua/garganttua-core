package com.garganttua.core.expression;

import com.garganttua.core.supply.ISupplier;

/**
 * Functional interface for evaluating expression leafs with direct object parameters.
 *
 * <p>An expression leaf is a terminal element in an expression tree that takes
 * direct object parameters (not suppliers) and produces a result supplier. Expression
 * leafs represent literals, constants, or simple value factories in the expression language.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an integer literal leaf
 * IEvaluateLeaf<Integer> intLeaf = (parameters) -> {
 *     return () -> Optional.of((Integer) parameters[0]);
 * };
 *
 * // Evaluate the leaf
 * ISupplier<Integer> result = intLeaf.evaluate(42);
 * assertEquals(42, result.supply().get());
 *
 * // String literal leaf (from tests)
 * IEvaluateLeaf<String> stringLeaf = (parameters) -> {
 *     return () -> Optional.of((String) parameters[0]);
 * };
 * ISupplier<String> str = stringLeaf.evaluate("hello");
 * assertEquals("hello", str.supply().get());
 * }</pre>
 *
 * @param <R> the type of value produced by this expression leaf
 * @see IEvaluateNode
 * @see IExpressionNode
 * @since 2.0.0-ALPHA01
 */
@FunctionalInterface
public interface IEvaluateLeaf<R> {

    /**
     * Evaluates this expression leaf with the given object parameters.
     *
     * @param parameters the direct object parameters (already evaluated, not suppliers)
     * @return a supplier that provides the evaluation result
     * @throws ExpressionException if evaluation fails
     */
    ISupplier<R> evaluate(Object ...parameters) throws ExpressionException;

}
