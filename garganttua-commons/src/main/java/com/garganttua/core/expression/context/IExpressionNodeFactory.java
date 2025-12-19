package com.garganttua.core.expression.context;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionNodeFactory<R, S extends ISupplier<R>> extends IContextualMethodBinder<IExpressionNode<R, S>, IExpressionNodeContext> {

    String key();

    String description();

    /**
     * Returns a manual page (man-style) documentation for this expression node factory.
     *
     * @return a formatted string containing comprehensive documentation
     */
    String man();

}
