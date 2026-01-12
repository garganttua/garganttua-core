package com.garganttua.core.expression.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

public interface IExpressionContextBuilder extends IAutomaticBuilder<IExpressionContextBuilder, IExpressionContext>, IPackageableBuilder<IExpressionContextBuilder, IExpressionContext>, IDependentBuilder<IExpressionContextBuilder, IExpressionContext> {

    /**
     * @param <T>
     * @param methodOwnerSupplier
     * @param supplied
     * @return
     */
    <T> IExpressionMethodBinderBuilder<T> expression(ISupplierBuilder<?, ? extends ISupplier<?>> methodOwnerSupplier, Class<T> supplied);



}
