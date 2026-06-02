package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

/**
 * Default {@link IExpression} implementation wrapping a root {@link IExpressionNode}.
 *
 * <p>Evaluation delegates to the root node, producing an {@link ISupplier} for lazy computation.
 * The static {@code evaluateNode} helpers dispatch on whether a node is contextual, supplying an
 * empty {@link com.garganttua.core.expression.context.ExpressionContext} when none is provided.
 *
 * @param <R> the type produced by this expression
 */
public class Expression<R> implements IExpression<R, ISupplier<R>> {
    private static final Logger log = Logger.getLogger(Expression.class);

    private IExpressionNode<R, ? extends ISupplier<R>> root;

    /**
     * Creates an expression rooted at the given node.
     *
     * @param root the root expression node (must not be {@code null})
     */
    public Expression(IExpressionNode<R, ? extends ISupplier<R>> root) {
        log.trace("Entering Expression constructor");
        this.root = Objects.requireNonNull(root, "Root expression cannot be null");
        log.debug("Expression created with root node type: {}", root.getClass().getSimpleName());
        log.trace("Exiting Expression constructor");
    }

    @Override
    public Type getSuppliedType() {
        log.trace("Getting supplied type from root node");
        return this.root.getSuppliedType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        log.trace("Entering evaluate()");
        log.debug("Evaluating expression");
        ISupplier<?> evaluation = Expression.evaluateNode(root);
        log.debug("Expression evaluated successfully");
        log.trace("Exiting evaluate()");
        return (ISupplier<R>) evaluation;
    }

    /**
     * Evaluates a node, supplying an empty {@link ExpressionContext} if the node is contextual.
     *
     * @param node the node to evaluate
     * @return the supplier produced by the node
     */
    public static ISupplier<?> evaluateNode(IExpressionNode<?, ?> node) {
        log.trace("Entering evaluateNode(node={})", node.getClass().getSimpleName());
        if (node instanceof IContextualExpressionNode<?,?> cNode) {
            log.debug("Node is contextual, evaluating with empty context");
            return cNode.evaluate(new ExpressionContext(Set.of()));
        }
        log.debug("Node is not contextual, evaluating directly");
        return node.evaluate();
    }

    /**
     * Evaluates a node against the given context, falling back to an empty
     * {@link ExpressionContext} when {@code context} is {@code null} and the node is contextual.
     *
     * @param node    the node to evaluate
     * @param context the expression context, may be {@code null}
     * @return the supplier produced by the node
     */
    public static ISupplier<?> evaluateNode(IExpressionNode<?, ?> node, IExpressionContext context) {
        log.trace("Entering evaluateNode(node={}, context={})",
                node.getClass().getSimpleName(), context != null ? context.getClass().getSimpleName() : "null");
        if (node instanceof IContextualExpressionNode<?,?> cNode) {
            if( context == null ) {
                log.debug("Context is null, creating empty context");
                context = new ExpressionContext(Set.of());
            }
            log.debug("Node is contextual, evaluating with provided context");
            return cNode.evaluate(context);
        }
        log.debug("Node is not contextual, evaluating directly");
        return node.evaluate();
    }

    @Override
    public boolean isContextual() {
        return root.isContextual();
    }

    @Override
    public IClass<R> getSuppliedClass() {
        return root.getFinalSuppliedClass();
    }
}
