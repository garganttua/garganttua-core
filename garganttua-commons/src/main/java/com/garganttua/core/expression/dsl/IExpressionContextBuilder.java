package com.garganttua.core.expression.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;

public interface IExpressionContextBuilder extends IAutomaticBuilder<IExpressionContextBuilder, IExpressionContext>, IPackageableBuilder<IExpressionContextBuilder, IExpressionContext> {

    <T> IExpressionMethodBinderBuilder<T> withExpressionNode(Class<?> methodOwner, Class<T> supplied);

    <T> IExpressionMethodBinderBuilder<T> withExpressionLeaf(Class<?> methodOwner, Class<T> supplied);

    IExpressionContextBuilder context(IDiContextBuilder context);

}
