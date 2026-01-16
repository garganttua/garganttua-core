package com.garganttua.core.expression.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.expression.context.IExpressionNodeFactory;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionMethodBinderBuilder<S> extends
        IMethodBinderBuilder<IExpressionNode<S, ISupplier<S>>, IExpressionMethodBinderBuilder<S>, IExpressionContextBuilder, IExpressionNodeFactory<S, ISupplier<S>>> {

    IExpressionMethodBinderBuilder<S> withNullableParam(int i);

    IExpressionMethodBinderBuilder<S> withName(String name);

    IExpressionMethodBinderBuilder<S> withDescription(String description);
    
    IExpressionMethodBinderBuilder<S> encapsulatedMethod(ObjectAddress methodAddress,
            Class<S> returnType, Class<?>... parameterTypes) throws DslException;

    IExpressionMethodBinderBuilder<S> encapsulatedMethod(String methodName,
            Class<S> returnType, Class<?>... parameterTypes) throws DslException;

}
