package com.garganttua.core.query;

import java.util.Objects;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IContextualExpressionNode;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.IExpressionContext;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.supply.ISupplier;

public class Expression<R> implements IExpression<R, ISupplier<R>> {

    private IExpressionNode<R, ISupplier<R>> leaf;

    public Expression(IExpressionNode<R, ISupplier<R>> leaf) {
        this.leaf = Objects.requireNonNull(leaf, "Leaf expression cannot be null");
    }

    @Override
    public Type getSuppliedType() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        ISupplier<?> evaluation = Expression.evaluateNode(leaf);
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
