package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

public class Expression<R> implements IExpression<R, ISupplier<R>> {
    private static final IDiagnostic log = Diagnostics.of(Expression.class);

    private IExpressionNode<R, ? extends ISupplier<R>> root;

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

    public static ISupplier<?> evaluateNode(IExpressionNode<?, ?> node) {
        log.trace("Entering evaluateNode(node={})", node.getClass().getSimpleName());
        if (node instanceof IContextualExpressionNode<?,?> cNode) {
            log.debug("Node is contextual, evaluating with empty context");
            return cNode.evaluate(new ExpressionContext(Set.of()));
        }
        log.debug("Node is not contextual, evaluating directly");
        return node.evaluate();
    }

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
