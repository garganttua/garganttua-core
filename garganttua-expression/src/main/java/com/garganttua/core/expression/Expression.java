package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.expression.context.ExpressionContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Expression<R> implements IExpression<R, ISupplier<R>> {

    private IExpressionNode<R, ? extends ISupplier<R>> root;

    public Expression(IExpressionNode<R, ? extends ISupplier<R>> root) {
        log.atTrace().log("Entering Expression constructor");
        this.root = Objects.requireNonNull(root, "Root expression cannot be null");
        log.atDebug().log("Expression created with root node type: {}", root.getClass().getSimpleName());
        log.atTrace().log("Exiting Expression constructor");
    }

    @Override
    public Type getSuppliedType() {
        log.atTrace().log("Getting supplied type from root node");
        return this.root.getSuppliedType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        log.atTrace().log("Entering evaluate()");
        log.atDebug().log("Evaluating expression");
        ISupplier<?> evaluation = Expression.evaluateNode(root);
        log.atDebug().log("Expression evaluated successfully");
        log.atTrace().log("Exiting evaluate()");
        return (ISupplier<R>) evaluation;
    }

    public static ISupplier<?> evaluateNode(IExpressionNode<?, ?> node) {
        log.atTrace().log("Entering evaluateNode(node={})", node.getClass().getSimpleName());
        if (node instanceof IContextualExpressionNode<?,?> cNode) {
            log.atDebug().log("Node is contextual, evaluating with empty context");
            return cNode.evaluate(new ExpressionContext(Set.of()));
        }
        log.atDebug().log("Node is not contextual, evaluating directly");
        return node.evaluate();
    }

    public static ISupplier<?> evaluateNode(IExpressionNode<?, ?> node, IExpressionContext context) {
        log.atTrace().log("Entering evaluateNode(node={}, context={})",
                node.getClass().getSimpleName(), context != null ? context.getClass().getSimpleName() : "null");
        if (node instanceof IContextualExpressionNode<?,?> cNode) {
            if( context == null ) {
                log.atDebug().log("Context is null, creating empty context");
                context = new ExpressionContext(Set.of());
            }
            log.atDebug().log("Node is contextual, evaluating with provided context");
            return cNode.evaluate(context);
        }
        log.atDebug().log("Node is not contextual, evaluating directly");
        return node.evaluate();
    }

    @Override
    public boolean isContextual() {
        return root.isContextual();
    }
}
