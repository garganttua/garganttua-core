package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

public class CustomCondition<T> implements ICondition  {
    private static final IDiagnostic log = Diagnostics.of(CustomCondition.class);

    private ISupplier<T> supplier;
    private Predicate<T> predicate;

    public CustomCondition(ISupplier<T> supplier,
            Predicate<T> predicate) {
        log.trace("Entering CustomCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.trace("Exiting CustomCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for CustomCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating CUSTOM condition - applying predicate to supplied value");
                Optional<T> value = supplier.supply();
                if (value.isEmpty()) {
                    log.error("Supplied value is empty or null");
                    throw new ConditionException("Supplied value is empty or null");
                }
                boolean result = predicate.test(value.get());
                log.debug("CUSTOM condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }
}
