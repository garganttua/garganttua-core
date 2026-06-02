package com.garganttua.core.expression.context;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.supply.ISupplier;

/**
 * Creates {@link IExpressionNode} instances from an {@link IExpressionNodeContext},
 * binding to the underlying factory method.
 *
 * @param <R> the type of value produced by nodes built by this factory
 * @param <S> the supplier type that provides the result
 * @since 2.0.0-ALPHA01
 */
public interface IExpressionNodeFactory<R, S extends ISupplier<R>> extends IContextualMethodBinder<IExpressionNode<R, S>, IExpressionNodeContext> {

    /**
     * Returns the unique key identifying this factory, in the format
     * {@code "functionName(Type1,Type2,...)"}.
     *
     * @return the factory key
     */
    String key();

    /**
     * Returns the human-readable description of this expression function.
     *
     * @return the description
     */
    String description();

    /**
     * Returns a manual page (man-style) documentation for this expression node factory.
     *
     * @return a formatted string containing comprehensive documentation
     */
    String man();

}
