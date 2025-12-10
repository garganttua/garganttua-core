package com.garganttua.core.expression.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.garganttua.core.supply.ISupplier;

public class ExpressionContext implements IExpressionContext {

    private Map<String, IExpressionNodeFactory<?,? extends ISupplier<?>>> nodes = new HashMap<>();

    public ExpressionContext(Set<IExpressionNodeFactory<?,? extends ISupplier<?>>> builtNodes) {
        //TODO Auto-generated constructor stub
    }

}
