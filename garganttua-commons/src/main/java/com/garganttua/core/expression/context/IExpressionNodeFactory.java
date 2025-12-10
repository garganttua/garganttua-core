package com.garganttua.core.expression.context;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionNodeFactory<R, S extends ISupplier<R>> extends IMethodBinder<IExpressionNode<R, S>>{

}
