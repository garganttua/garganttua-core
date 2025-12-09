package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EqualsCondition<T> implements ICondition {

    private ISupplier<T> supplier1;
    private ISupplier<T> supplier2;

    public EqualsCondition(ISupplier<T> supplier1, ISupplier<T> supplier2) {
        log.atTrace().log("Entering EqualsCondition constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 cannot be null");
        if (!this.supplier1.getSuppliedClass().equals(this.supplier2.getSuppliedClass())) {
            log.atError().log("Type mismatch: {} VS {}",
                this.supplier1.getSuppliedClass().getSimpleName(),
                this.supplier2.getSuppliedClass().getSimpleName());
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedClass().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedClass().getSimpleName());
        }
        log.atTrace().log("Exiting EqualsCondition constructor");
    }

    /*
        TODO: this method do a full evaluation, find a way to delegate the effective evaluation within the returned supplier
    */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
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
        return new FixedSupplier<Boolean>(result);
    }

}
