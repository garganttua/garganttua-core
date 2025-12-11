package com.garganttua.core.expression.context;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionNodeFactory<R, S extends ISupplier<R>> extends IContextualMethodBinder<IExpressionNode<R, S>, IExpressionNodeContext> {

    String getKey();

}
