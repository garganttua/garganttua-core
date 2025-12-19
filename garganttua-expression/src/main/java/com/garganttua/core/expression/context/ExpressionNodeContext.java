package com.garganttua.core.expression.context;

import java.util.List;
import java.util.Objects;

import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.supply.IContextualSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionNodeContext implements IExpressionNodeContext {

    private List<Object> parameters;

    public ExpressionNodeContext(List<Object> parameters) {
        this.parameters = Objects.requireNonNull(parameters, "Parameters list cannot be null");
    }

    
    @Override
    public boolean buildContextual() {
        return this.parameters.stream().anyMatch(s -> IContextualSupplier.class.isAssignableFrom(s.getClass()));
    }

    @Override
    public boolean matches(Class<?>[] parameterTypes) {
        if (parameterTypes.length != parameters().size()) {
            log.atWarn()
                    .log("Expression leaf is expecting " + parameterTypes.length + " parameters, but context contains "
                            + parameters().size());
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {

            if( parameters().get(i) instanceof IExpressionNode<?, ?> node ) {
                if (!parameterTypes[i].isAssignableFrom(node.getFinalSuppliedClass())) {
                    log.atWarn()
                            .log("Expression node is expecting parameter " + i + " of type "
                                    + parameterTypes[i].getSimpleName() + " but context provided "
                                    + node.getFinalSuppliedClass().getSimpleName());
                    return false;
                }
            } else {
                if (!parameterTypes[i].isAssignableFrom(parameters().get(i).getClass())) {
                    log.atWarn()
                            .log("Expression node is expecting parameter " + i + " of type "
                                    + parameterTypes[i].getSimpleName() + " but context provided "
                                    + parameters().get(i).getClass().getSimpleName());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public List<Object> parameters() {
        return this.parameters;
    }
}
