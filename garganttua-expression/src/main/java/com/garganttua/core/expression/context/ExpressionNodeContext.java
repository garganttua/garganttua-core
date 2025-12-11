package com.garganttua.core.expression.context;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionNodeContext implements IExpressionNodeContext {

    private List<IExpressionNode<?, ? extends ISupplier<?>>> nodeChilds;
    private Boolean leaf;
    private List<ISupplier<?>> leafParameters;

    public ExpressionNodeContext(List<?> parameters) {
        this(parameters, false);
    }

    @SuppressWarnings("unchecked")
    public ExpressionNodeContext(List<?> parameters, Boolean leaf) {
        this.leaf = leaf;
        Objects.requireNonNull(parameters, "Parameters list cannot be null");

        if (parameters.isEmpty()) {
            this.nodeChilds = Collections.emptyList();
            this.leafParameters = Collections.emptyList();
            return;
        }

        boolean allAreSuppliers = parameters.stream().allMatch(IExpressionNode.class::isInstance);

        if (!leaf) {
            if (!allAreSuppliers) {
                throw new ExpressionException(
                        "Expression node should be provided with only IExpressionNode parameters");
            }
            this.nodeChilds = (List<IExpressionNode<?, ? extends ISupplier<?>>>) parameters;
        } else {
            if (allAreSuppliers) {
                throw new ExpressionException(
                        "Expression leaf should be provided with only non IExpressionNode parameters");
            }
            this.leafParameters = parameters.stream()
                    .map(FixedSupplier::new)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<ISupplier<?>> parameters() {
        return this.leafParameters;
    }

    @Override
    public Object[] leafParameters() {
        if (leaf) {
            return this.leafParameters.stream().map(ISupplier::supply).map(Optional::get).collect(Collectors.toList()).toArray();
        } else {
            return new Object[0];
            // Log
        }
    }

    @Override
    public boolean buildContextual() {
        return !this.leaf
                || this.nodeChilds.stream().anyMatch(s -> IContextualSupplier.class.isAssignableFrom(s.getClass()));
    }

    @Override
    public List<IExpressionNode<?, ? extends ISupplier<?>>> nodeChilds() {
        return this.nodeChilds;
    }

    @Override
    public boolean matches(Class<?>[] parameterTypes) {
        if (leaf)
            return leafMatches(parameterTypes);
        return nodeMatches(parameterTypes);
    }

    private boolean nodeMatches(Class<?>[] parameterTypes) {
        return true;
    }

    private Boolean leafMatches(Class<?>[] parameterTypes) throws ExpressionException {

        if (parameterTypes.length != parameters().size()) {
            log.atWarn()
                    .log("Expression leaf is expecting " + parameterTypes.length + " parameters, but context contains "
                            + parameters().size());
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!parameterTypes[i].isAssignableFrom(parameters().get(i).getSuppliedClass())) {
                log.atWarn()
                        .log("Expression node is expecting parameter " + i + " of type "
                                + parameterTypes[i].getSimpleName() + " but context provided "
                                + parameters().get(i).getSuppliedClass().getSimpleName());
                return false;
            }
        }

        return true;
    }
}
