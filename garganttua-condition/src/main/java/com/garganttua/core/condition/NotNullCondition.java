package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotNullCondition implements ICondition {

    private ISupplier<?> supplier;

    public NotNullCondition(ISupplier<?> supplier) {
        log.atTrace().log("Entering NotNullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.atTrace().log("Exiting NotNullCondition constructor");
    }

    /*
        TODO: this method do a full evaluation, find a way to delegate the effective evaluation within the returned supplier
    */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NotNullCondition");
        log.atDebug().log("Evaluating NOT NULL condition - negation of NULL condition");

        boolean nullResult = new NullCondition(supplier).fullEvaluate();
        log.atDebug().log("NULL condition result: {}", nullResult);

        boolean result = !nullResult;
        log.atInfo().log("NOT NULL condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
    }

}
