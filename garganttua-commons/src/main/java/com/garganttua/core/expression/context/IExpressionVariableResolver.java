package com.garganttua.core.expression.context;

import java.util.Optional;

@FunctionalInterface
public interface IExpressionVariableResolver {

    <T> Optional<T> resolve(String name, Class<T> type);
}
