package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotEqualsCondition<T> implements ICondition {

    private IObjectSupplier<T> supplier1;
    private IObjectSupplier<T> supplier2;

    public NotEqualsCondition(IObjectSupplier<T> supplier1, IObjectSupplier<T> supplier2) {
        log.atTrace().log("Entering NotEqualsCondition constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 cannot be null");
        if (!this.supplier1.getSuppliedType().equals(this.supplier2.getSuppliedType())) {
            log.atError().log("Type mismatch: {} VS {}",
                this.supplier1.getSuppliedType().getSimpleName(),
                this.supplier2.getSuppliedType().getSimpleName());
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedType().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedType().getSimpleName());
        }
        log.atTrace().log("Exiting NotEqualsCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NotEqualsCondition");
        log.atDebug().log("Evaluating NOT EQUALS condition - negation of EQUALS condition");

        boolean equalsResult = new EqualsCondition<>(supplier1, supplier2).evaluate();
        log.atDebug().log("EQUALS condition result: {}", equalsResult);

        boolean result = !equalsResult;
        log.atInfo().log("NOT EQUALS condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }

}
