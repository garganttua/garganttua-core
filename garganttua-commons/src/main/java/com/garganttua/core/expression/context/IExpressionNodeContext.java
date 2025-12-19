package com.garganttua.core.expression.context;

import java.util.List;

public interface IExpressionNodeContext {

    /**
     * Can contains either IExpressionNode or direct parameters 
     * @return
     */
    List<Object> parameters();

    /* Object[] leafParameters(); */

    boolean buildContextual();

   /*  List<IExpressionNode<?, ? extends ISupplier<?>>> nodeChilds(); */

    boolean matches(Class<?>[] parameterTypes);

}
