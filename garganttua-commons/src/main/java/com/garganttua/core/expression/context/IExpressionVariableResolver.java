package com.garganttua.core.expression.context;

import java.util.Optional;

import com.garganttua.core.reflection.IClass;

/**
 * Resolves named variables to typed values during expression evaluation.
 *
 * @since 2.0.0-ALPHA01
 */
@FunctionalInterface
public interface IExpressionVariableResolver {

    /**
     * Resolves a variable by name to a value of the requested type.
     *
     * @param name the variable name (without the {@code @} or {@code .} prefix)
     * @param type the expected value type
     * @param <T>  the value type
     * @return the resolved value, or empty if the variable is undefined
     */
    <T> Optional<T> resolve(String name, IClass<T> type);
}
