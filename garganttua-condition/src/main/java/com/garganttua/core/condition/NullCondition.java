package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullCondition implements ICondition {

    private ISupplier<?> supplier;

    public NullCondition(ISupplier<?> supplier) {
        log.atTrace().log("Entering NullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.atTrace().log("Exiting NullCondition constructor");
    }

    /*
        TODO: this method do a full evaluation, find a way to delegate the effective evaluation within the returned supplier
    */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NullCondition");
        log.atDebug().log("Evaluating NULL condition - checking if supplier returns null/empty");

        try {
            Optional<?> supplied = this.supplier.supply();
            log.atDebug().log("Supplier returned: {}", supplied.isPresent() ? "non-empty value" : "empty/null");
            if (supplied.isPresent()) {
                log.atInfo().log("NULL condition evaluation complete: false (value is present)");
                log.atTrace().log("Exiting evaluate() with result: false");
                return new FixedSupplier<Boolean>(false);
            }

        } catch (Exception e) {
            log.atWarn().log("Exception during supplier evaluation, treating as null: {}", e.getMessage());
            log.atInfo().log("NULL condition evaluation complete: true (exception occurred)");
            log.atTrace().log("Exiting evaluate() with result: true");
            return new FixedSupplier<Boolean>(true);
        }

        log.atInfo().log("NULL condition evaluation complete: true (value is null/empty)");
        log.atTrace().log("Exiting evaluate() with result: true");
        return new FixedSupplier<Boolean>(true);
    }

}
