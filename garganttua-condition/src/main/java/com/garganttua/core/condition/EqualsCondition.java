package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EqualsCondition<T> implements ICondition {

    private IObjectSupplier<T> supplier1;
    private IObjectSupplier<T> supplier2;

    public EqualsCondition(IObjectSupplier<T> supplier1, IObjectSupplier<T> supplier2) {
        log.atTrace().log("Entering EqualsCondition constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 cannot be null");
        if (!this.supplier1.getSuppliedType().equals(this.supplier2.getSuppliedType())) {
            log.atError().log("Type mismatch: {} VS {}",
                this.supplier1.getSuppliedType().getSimpleName(),
                this.supplier2.getSuppliedType().getSimpleName());
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedType().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedType().getSimpleName());
        }
        log.atTrace().log("Exiting EqualsCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for EqualsCondition");
        log.atDebug().log("Evaluating EQUALS condition - comparing two supplied values");

        this.supplier1.supply().orElseThrow(() -> {
            log.atError().log("Supplier 1 supplied empty value");
            return new ConditionException("Supplier 1 supplied empty value");
        });
        log.atDebug().log("Supplier 1 provided a non-empty value");

        this.supplier2.supply().orElseThrow(() -> {
            log.atError().log("Supplier 2 supplied empty value");
            return new ConditionException("Supplier 2 supplied empty value");
        });
        log.atDebug().log("Supplier 2 provided a non-empty value");

        boolean result = this.supplier1.supply().get().equals(this.supplier2.supply().get());
        log.atInfo().log("EQUALS condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }

}
