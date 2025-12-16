package com.garganttua.core.expression;

import java.util.Optional;

import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Expression node that can be evaluated to produce a supplier of values.
 *
 * <p>An expression node represents a parsed and ready-to-evaluate element in an
 * expression tree. It combines the evaluation capability with the supplier pattern,
 * enabling lazy evaluation and integration with the Garganttua supply framework.</p>
 *
 * <h2>Key Characteristics</h2>
 * <ul>
 *   <li>Extends {@code ISupplier<S>} where S is itself a supplier of the final result</li>
 *   <li>Evaluation produces a supplier, not a direct value</li>
 *   <li>Supports lazy, deferred execution of expressions</li>
 *   <li>Type-safe with generic parameters for result type</li>
 * </ul>
 *
 * <h2>Usage Example (from tests)</h2>
 * <pre>{@code
 * // Parse expression to get expression node
 * IExpressionContext context = buildContext();
 * String expr = "add(10, 20)";
 *
 * // Expression node evaluation
 * IExpression<Integer, ISupplier<Integer>> expression = context.expression(expr);
 * ISupplier<Integer> resultSupplier = expression.evaluate();
 *
 * // Lazy evaluation - value computed when needed
 * Integer result = resultSupplier.supply().get();
 * assertEquals(30, result);
 * }</pre>
 *
 * <h2>Design Pattern</h2>
 * <p>This interface implements a double-supplier pattern:</p>
 * <ol>
 *   <li>First level: {@code ISupplier<S>} - the expression node itself is a supplier</li>
 *   <li>Second level: {@code S extends ISupplier<R>} - the evaluation result is also a supplier</li>
 * </ol>
 * <p>This enables maximum flexibility for lazy evaluation and composition.</p>
 *
 * @param <R> the final type of value produced when fully evaluated
 * @param <S> the supplier type that provides the final result
 * @see IExpression
 * @see IContextualExpressionNode
 * @see com.garganttua.core.supply.ISupplier
 * @since 2.0.0-ALPHA01
 */
public interface IExpressionNode<R, S extends ISupplier<R>> extends ISupplier<S> {

    /**
     * Evaluates this expression node to produce a result supplier.
     *
     * @return a supplier that provides the expression result
     * @throws ExpressionException if evaluation fails
     */
    S evaluate() throws ExpressionException;

    /**
     * Supplies the result supplier by delegating to {@link #evaluate()}.
     *
     * @return an Optional containing the result supplier
     * @throws SupplyException if evaluation fails
     */
    @Override
    default Optional<S> supply() throws SupplyException {
        return Optional.of(this.evaluate());
    }

    /**
     * Returns the final class type produced by this expression.
     *
     * @return the class of the final result value
     */
    Class<R> getFinalSuppliedClass();

}
