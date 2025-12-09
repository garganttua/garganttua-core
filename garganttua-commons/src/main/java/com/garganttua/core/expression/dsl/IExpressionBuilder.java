package com.garganttua.core.expression.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

public interface IExpressionBuilder<R, S extends ISupplier<R>> extends IAutomaticBuilder<IExpressionBuilder<R, S>, IExpression<R, S>>, IPackageableBuilder<IExpressionBuilder<R, S>, IExpression<R, S>>, ISupplierBuilder<S, IExpression<R, S>>{

    <T> IExpressionMethodBinderBuilder<T> withExpression(Class<?> methodOwner, Class<T> supplied);

}
