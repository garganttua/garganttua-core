package com.garganttua.core.expression.dsl;

import com.garganttua.core.expression.IExpressionMethodBinder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionMethodBinderBuilder<S> extends IMethodBinderBuilder<S, IExpressionMethodBinderBuilder<S>, IExpressionBuilder<S, ISupplier<S>>, IExpressionMethodBinder<S>>{

}
