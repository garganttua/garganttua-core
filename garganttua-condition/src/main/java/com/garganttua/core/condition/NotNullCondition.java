package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.supply.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotNullCondition implements ICondition {

    private IObjectSupplier<?> supplier;

    public NotNullCondition(IObjectSupplier<?> supplier) {
        log.atTrace().log("Entering NotNullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.atTrace().log("Exiting NotNullCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NotNullCondition");
        log.atDebug().log("Evaluating NOT NULL condition - negation of NULL condition");

        boolean nullResult = new NullCondition(supplier).evaluate();
        log.atDebug().log("NULL condition result: {}", nullResult);

        boolean result = !nullResult;
        log.atInfo().log("NOT NULL condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }

}
