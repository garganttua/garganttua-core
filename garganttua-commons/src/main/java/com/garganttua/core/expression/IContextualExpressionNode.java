package com.garganttua.core.expression;

import java.util.Optional;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Contextual expression node that requires an expression context for evaluation.
 *
 * <p>This interface combines {@link IExpressionNode} with {@link IContextualSupplier},
 * enabling expression nodes that need access to the expression context during evaluation.
 * Contextual expression nodes can reference variables, call other expressions, or access
 * context-specific state.</p>
 *
 * <h2>Key Characteristics</h2>
 * <ul>
 *   <li>Extends {@code IExpressionNode} for expression tree integration</li>
 *   <li>Implements {@code IContextualSupplier} for context-aware evaluation</li>
 *   <li>Requires {@code IExpressionContext} as owner context</li>
 *   <li>Throws exception when evaluated without context</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a contextual expression node for variable reference
 * IContextualExpressionNode<String, IContextualSupplier<String, IExpressionContext>> varNode =
 *     new IContextualExpressionNode<>() {
 *         @Override
 *         public IContextualSupplier<String, IExpressionContext> evaluate(
 *                 IExpressionContext context, Object... otherContexts) {
 *             return context.resolveVariable("userName", String.class);
 *         }
 *
 *         @Override
 *         public Class<String> getFinalSuppliedClass() {
 *             return String.class;
 *         }
 *     };
 *
 * // Evaluate with context
 * IExpressionContext ctx = buildContext();
 * IContextualSupplier<String, IExpressionContext> result = varNode.evaluate(ctx);
 * String value = result.supply(ctx).get();
 * }</pre>
 *
 * <h2>Default Behavior</h2>
 * <p>This interface provides default implementations that:</p>
 * <ul>
 *   <li>{@code evaluate()} throws exception (context required)</li>
 *   <li>{@code supply()} delegates to contextual supply</li>
 *   <li>{@code getOwnerContextType()} returns {@code IExpressionContext.class}</li>
 * </ul>
 *
 * @param <R> the final type of value produced by this expression
 * @param <S> the contextual supplier type for this expression
 * @see IExpressionNode
 * @see IContextualSupplier
 * @see IExpressionContext
 * @since 2.0.0-ALPHA01
 */
public interface IContextualExpressionNode<R, S extends IContextualSupplier<R, IExpressionContext>>
        extends IExpressionNode<R, S>, IContextualSupplier<S, IExpressionContext> {

    S evaluate(IExpressionContext ownerContext,
            Object... otherContexts) throws ExpressionException;

    @Override
    default Optional<S> supply(IExpressionContext ownerContext, Object... otherContexts) throws SupplyException {
        return Optional.of(this.evaluate(ownerContext, otherContexts));
    }

    @Override
    default S evaluate() throws ExpressionException {
        throw new ExpressionException(
                "Owner context of type " + getOwnerContextType().getSimpleName() + " required for this expression");
    }

    @Override
    default Optional<S> supply() throws ExpressionException {
        return this.supply();
    }

    @Override
        default Class<IExpressionContext> getOwnerContextType() {
        return IExpressionContext.class;
        }

}
