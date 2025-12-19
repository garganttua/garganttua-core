package com.garganttua.core.expression;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;

/**
 * Interface for evaluating contextual expression nodes that require an expression context.
 *
 * <p>Contextual expressions have access to the expression context during evaluation,
 * enabling dynamic behavior such as variable resolution, dependency injection, or
 * accessing registered expression functions. This is used for expressions that need
 * to reference other expressions or context-specific state.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Variable references in expressions (accessing named values from context)</li>
 *   <li>Nested expression evaluation (expressions that call other expressions)</li>
 *   <li>Dynamic function resolution (looking up functions by name at evaluation time)</li>
 *   <li>Context-aware computations (expressions that depend on context state)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Contextual expression that accesses variables from context
 * IContextualEvaluate<String> varRef = (context, parameters) -> {
 *     String varName = (String) parameters[0].supply().get();
 *     return context.resolveVariable(varName, String.class);
 * };
 *
 * // Evaluate with context
 * IExpressionContext ctx = createContext();
 * ISupplier<String> varNameSupplier = () -> Optional.of("userName");
 * IContextualSupplier<String, IExpressionContext> result =
 *     varRef.evaluate(ctx, varNameSupplier);
 * String value = result.supply(ctx).get();
 * }</pre>
 *
 * @param <R> the type of value produced by this contextual evaluation
 * @see IExpressionContext
 * @see IContextualExpressionNode
 * @since 2.0.0-ALPHA01
 */
public interface IContextualEvaluate<R> {

    /**
     * Evaluates this contextual expression with the given context and supplier parameters.
     *
     * @param context the expression context providing access to variables and functions
     * @param parameters the supplier parameters to this evaluation (lazily evaluated)
     * @return a contextual supplier that requires the expression context for evaluation
     * @throws ExpressionException if evaluation fails
     */
    IContextualSupplier<R, IExpressionContext> evaluate(IExpressionContext context, Object ...parameters) throws ExpressionException;

}
