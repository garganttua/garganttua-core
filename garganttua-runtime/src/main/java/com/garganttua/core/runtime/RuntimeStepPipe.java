package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

/**
 * A conditional pipe applied to a step's result: when {@code condition} is absent
 * or evaluates {@code true}, the {@code handler} expression produces a replacement
 * value, optionally setting an exit code and/or storing into a variable.
 *
 * @param condition    the optional gating condition (absent means always-match)
 * @param handler      the expression producing the replacement value
 * @param code         the optional exit code to set when the pipe matches
 * @param variableName the optional variable to store the handler result in
 */
public record RuntimeStepPipe(
        Optional<IExpression<Boolean, ? extends ISupplier<Boolean>>> condition,
        IExpression<?, ? extends ISupplier<?>> handler,
        Optional<Integer> code,
        Optional<String> variableName) implements IRuntimeStepPipe {

    /**
     * Convenience constructor for a pipe without a target variable.
     *
     * @param condition the optional gating condition
     * @param handler   the expression producing the replacement value
     * @param code      the optional exit code to set when the pipe matches
     */
    public RuntimeStepPipe(
            Optional<IExpression<Boolean, ? extends ISupplier<Boolean>>> condition,
            IExpression<?, ? extends ISupplier<?>> handler,
            Optional<Integer> code) {
        this(condition, handler, code, Optional.empty());
    }
}
