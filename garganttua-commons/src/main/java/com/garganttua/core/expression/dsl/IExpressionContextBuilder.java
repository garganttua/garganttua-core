package com.garganttua.core.expression.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.expression.IExpressionContext;

public interface IExpressionContextBuilder extends IAutomaticBuilder<IExpressionContextBuilder, IExpressionContext>, IPackageableBuilder<IExpressionContextBuilder, IExpressionContext> {

    <T> IExpressionMethodBinderBuilder<T> withExpression(Class<?> methodOwner, Class<T> supplied);

}
