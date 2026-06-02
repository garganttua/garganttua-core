package com.garganttua.core.expression.context;

import java.util.List;

/**
 * A user-defined script function with named parameters that can be invoked at runtime.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IScriptFunction {

    /**
     * Returns the declared parameter names, in order.
     *
     * @return the parameter names
     */
    List<String> parameters();

    /**
     * Invokes this function with the given positional arguments.
     *
     * @param args the argument values, matched positionally to {@link #parameters()}
     * @return the function result, or {@code null} if it produces none
     */
    Object invoke(Object... args);
}
