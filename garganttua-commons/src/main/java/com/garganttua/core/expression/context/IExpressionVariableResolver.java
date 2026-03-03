package com.garganttua.core.expression.context;

import java.util.Optional;

import com.garganttua.core.reflection.IClass;

@FunctionalInterface
public interface IExpressionVariableResolver {

    <T> Optional<T> resolve(String name, IClass<T> type);
}
