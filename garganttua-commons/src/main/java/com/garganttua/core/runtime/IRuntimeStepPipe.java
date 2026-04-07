package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

/**
 * Defines conditional post-execution routing for a runtime step.
 *
 * <p>
 * A pipe evaluates a condition after the step's main expression has executed.
 * If the condition is met, the pipe's handler expression is executed and its
 * result replaces the step's result. An optional code can be set on match.
 * </p>
 *
 * <p>
 * Pipes are evaluated sequentially. The first matching pipe wins — subsequent
 * pipes are skipped. A pipe with no condition (default pipe) always matches.
 * </p>
 *
 * <h2>Script Syntax</h2>
 * <pre>{@code
 * expression
 *     | condition1 => handler1 -> 100
 *     | condition2 => handler2 -> 200
 *     | => defaultHandler      -> 300
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IRuntimeStepMethodBinder
 */
public interface IRuntimeStepPipe {

    /**
     * Returns the condition to evaluate after step execution.
     *
     * <p>
     * The condition expression must return a {@code Boolean}. If the condition
     * is empty, this is a default pipe that always matches.
     * </p>
     *
     * @return the condition expression, or empty for a default (catch-all) pipe
     */
    Optional<IExpression<Boolean, ? extends ISupplier<Boolean>>> condition();

    /**
     * Returns the handler expression to execute when the condition matches.
     *
     * @return the handler expression
     */
    IExpression<?, ? extends ISupplier<?>> handler();

    /**
     * Returns the result code to set when this pipe matches.
     *
     * @return the code to set, or empty if no code change
     */
    Optional<Integer> code();

    /**
     * Returns the variable name to store the handler result in.
     *
     * @return the variable name, or empty if no variable assignment
     */
    Optional<String> variableName();

}
