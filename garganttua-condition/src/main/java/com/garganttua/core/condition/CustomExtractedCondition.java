package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.supplying.IObjectSupplier;

public class CustomExtractedCondition<T, R> implements ICondition {

    private IObjectSupplier<T> supplier;
    private Function<T, R> extractor;
    private Predicate<R> predicate;

    public CustomExtractedCondition(IObjectSupplier<T> supplier,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        Optional<T> value = supplier.supply();
        if (value.isEmpty()) throw new ConditionException("Supplied value is empty or null"); 
        R extracted = extractor.apply(value.get());
        return predicate.test(extracted);
    }

}
