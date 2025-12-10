package com.garganttua.core.expression;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.supply.ISupplier;

public class Expression<R> implements IExpression<R, ISupplier<R>> {

    private IExpressionNode<R, ? extends ISupplier<R>> root;

    public Expression(IExpressionNode<R, ? extends ISupplier<R>> root) {
        this.root = Objects.requireNonNull(root, "Root expression cannot be null");
    }

    @Override
    public Type getSuppliedType() {
        return this.root.getSuppliedType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        ISupplier<?> evaluation = Expression.evaluateNode(root);
        return (ISupplier<R>) evaluation;
    }

    public static ISupplier<?> evaluateNode(IExpressionNode<?, ?> node) {
        if (node instanceof IContextualExpressionNode cNode) {
            return cNode.evaluate(new ExpressionContext());
        }
        return (ISupplier<?>) node.evaluate();
    }

    public static ISupplier<?> evaluateNode(IExpressionNode<?, ?> node, IExpressionContext context) {
        if (node instanceof IContextualExpressionNode cNode) {
            if( context == null )
                context = new ExpressionContext();
            return cNode.evaluate(context);
        }
        return (ISupplier<?>) node.evaluate();
    }
}
