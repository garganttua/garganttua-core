package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.garganttua.core.supply.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomCondition<T> implements ICondition  {

    private IObjectSupplier<T> supplier;
    private Predicate<T> predicate;

    public CustomCondition(IObjectSupplier<T> supplier,
            Predicate<T> predicate) {
        log.atTrace().log("Entering CustomCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.atTrace().log("Exiting CustomCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for CustomCondition");
        log.atDebug().log("Evaluating CUSTOM condition - applying predicate to supplied value");

        Optional<T> value = supplier.supply();
        if (value.isEmpty()) {
            log.atError().log("Supplied value is empty or null");
            throw new ConditionException("Supplied value is empty or null");
        }
        log.atDebug().log("Supplier provided non-empty value");

        boolean result = predicate.test(value.get());
        log.atDebug().log("Predicate test result: {}", result);

        log.atInfo().log("CUSTOM condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }
}
