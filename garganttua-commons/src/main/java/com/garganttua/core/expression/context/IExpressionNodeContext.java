package com.garganttua.core.expression.context;

import java.util.List;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.supply.ISupplier;

public interface IExpressionNodeContext {

    List<ISupplier<?>> parameters();

    Object[] leafParameters();

    boolean buildContextual();

    List<IExpressionNode<?, ? extends ISupplier<?>>> nodeChilds();

    boolean matches(Class<?>[] parameterTypes);

}
