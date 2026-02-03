package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomExtractedCondition<T, R> implements ICondition {

    private ISupplier<T> supplier;
    private Function<T, R> extractor;
    private Predicate<R> predicate;

    public CustomExtractedCondition(ISupplier<T> supplier,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.atTrace().log("Entering CustomExtractedCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.atTrace().log("Exiting CustomExtractedCondition constructor");
    }

    /*
        TODO: this method do a full evaluation, find a way to delegate the effective evaluation within the returned supplier
    */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for CustomExtractedCondition");
        log.atDebug().log("Evaluating CUSTOM EXTRACTED condition - applying extractor then predicate");

        Optional<T> value = supplier.supply();
        if (value.isEmpty()) {
            log.atError().log("Supplied value is empty or null");
            throw new ConditionException("Supplied value is empty or null");
        }
        log.atDebug().log("Supplier provided non-empty value");

        R extracted = extractor.apply(value.get());
        log.atDebug().log("Extractor applied successfully");

        boolean result = predicate.test(extracted);
        log.atDebug().log("Predicate test result: {}", result);

        log.atDebug().log("CUSTOM EXTRACTED condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
    }

}
