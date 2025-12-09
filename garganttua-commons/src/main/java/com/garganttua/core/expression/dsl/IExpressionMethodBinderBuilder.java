package com.garganttua.core.expression.dsl;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.IExpressionNodeFactory;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionMethodBinderBuilder<S> extends IMethodBinderBuilder<IExpressionNode<S, ISupplier<S>>, IExpressionMethodBinderBuilder<S>, IExpressionContextBuilder, IExpressionNodeFactory<S, ISupplier<S>>>{

}
