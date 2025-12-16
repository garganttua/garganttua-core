package com.garganttua.core.expression;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Expression interface that combines evaluation with the supplier builder pattern.
 *
 * <p>An expression represents a parsed expression string that can be evaluated to
 * produce a supplier of values. It integrates with the DSL builder framework by
 * extending {@code ISupplierBuilder}, allowing expressions to be used anywhere
 * suppliers are built.</p>
 *
 * <h2>Key Characteristics</h2>
 * <ul>
 *   <li>Extends {@code ISupplierBuilder<R, S>} for DSL integration</li>
 *   <li>Provides {@code evaluate()} method for expression evaluation</li>
 *   <li>Default {@code build()} delegates to {@code evaluate()}</li>
 *   <li>Can be used in fluent builder chains</li>
 * </ul>
 *
 * <h2>Usage Example (from ExpressionContextTest)</h2>
 * <pre>{@code
 * // Parse expression string
 * IExpressionContext context = buildExpressionContext();
 * IExpression<String, ISupplier<String>> expr = context.expression("\"hello\"");
 *
 * // Evaluate expression
 * ISupplier<String> result = expr.evaluate();
 * assertEquals("hello", result.supply().get());
 *
 * // Or use as builder
 * ISupplier<String> result2 = expr.build();
 * assertEquals("hello", result2.supply().get());
 * }</pre>
 *
 * <h2>Integration with DSL</h2>
 * <p>Because {@code IExpression} extends {@code ISupplierBuilder}, expressions
 * can be composed with other builders in the Garganttua framework:</p>
 * <pre>{@code
 * IExpression<Integer, ISupplier<Integer>> numberExpr = context.expression("42");
 * ISupplier<Integer> supplier = numberExpr.build(); // Builder pattern
 * }</pre>
 *
 * @param <R> the final type of value produced by this expression
 * @param <S> the supplier type that provides the final result
 * @see IExpressionNode
 * @see com.garganttua.core.supply.dsl.ISupplierBuilder
 * @since 2.0.0-ALPHA01
 */
public interface IExpression<R, S extends ISupplier<R>> extends ISupplierBuilder<R, S> {

    /**
     * Evaluates this expression to produce a result supplier.
     *
     * @return a supplier that provides the expression result
     * @throws ExpressionException if evaluation fails
     */
    S evaluate() throws ExpressionException;

    /**
     * Builds the supplier by delegating to {@link #evaluate()}.
     *
     * <p>This default implementation enables expressions to be used in builder chains
     * by providing the standard {@code build()} method from {@code ISupplierBuilder}.</p>
     *
     * @return a supplier that provides the expression result
     * @throws DslException if evaluation fails (wraps {@code ExpressionException})
     */
    @Override
    default S build() throws DslException {
        return this.evaluate();
    }
}
