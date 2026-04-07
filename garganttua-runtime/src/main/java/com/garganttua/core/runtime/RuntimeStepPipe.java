package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

public record RuntimeStepPipe(
        Optional<IExpression<Boolean, ? extends ISupplier<Boolean>>> condition,
        IExpression<?, ? extends ISupplier<?>> handler,
        Optional<Integer> code,
        Optional<String> variableName) implements IRuntimeStepPipe {

    public RuntimeStepPipe(
            Optional<IExpression<Boolean, ? extends ISupplier<Boolean>>> condition,
            IExpression<?, ? extends ISupplier<?>> handler,
            Optional<Integer> code) {
        this(condition, handler, code, Optional.empty());
    }
}
