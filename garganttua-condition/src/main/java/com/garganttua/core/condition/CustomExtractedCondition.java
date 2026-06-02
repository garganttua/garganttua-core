package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

/**
 * Condition that extracts a value from the object produced by a supplier and
 * tests the extracted value against a {@link Predicate}.
 *
 * @param <T> the supplied source type
 * @param <R> the extracted value type
 */
public class CustomExtractedCondition<T, R> implements ICondition {
    private static final Logger log = Logger.getLogger(CustomExtractedCondition.class);

    private ISupplier<T> supplier;
    private Function<T, R> extractor;
    private Predicate<R> predicate;

    /**
     * Creates a custom extracted condition.
     *
     * @param supplier  supplier of the source value; must not be {@code null}
     * @param extractor function deriving the value to test; must not be {@code null}
     * @param predicate predicate applied to the extracted value; must not be {@code null}
     */
    public CustomExtractedCondition(ISupplier<T> supplier,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.trace("Entering CustomExtractedCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.trace("Exiting CustomExtractedCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for CustomExtractedCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating CUSTOM EXTRACTED condition - applying extractor then predicate");
                Optional<T> value = supplier.supply();
                if (value.isEmpty()) {
                    log.error("Supplied value is empty or null");
                    throw new ConditionException("Supplied value is empty or null");
                }
                R extracted = extractor.apply(value.get());
                boolean result = predicate.test(extracted);
                log.debug("CUSTOM EXTRACTED condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

}
