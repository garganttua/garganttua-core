package com.garganttua.core.expression.context;

import java.util.List;

import com.garganttua.core.reflection.IClass;

public interface IExpressionNodeContext {

    /**
     * Can contains either IExpressionNode or direct parameters
     * @return
     */
    List<Object> parameters();

    boolean buildContextual();

    boolean matches(IClass<?>[] parameterTypes);

    IClass<?>[] parameterTypes();

}
