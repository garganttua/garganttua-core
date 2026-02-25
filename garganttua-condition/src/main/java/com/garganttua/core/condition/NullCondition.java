package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.expression.annotations.Expression;
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

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NullCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.atDebug().log("Evaluating NULL condition - checking if supplier returns null/empty");
                Boolean result = Null(supplier.supply().orElse(null));
                log.atDebug().log("NULL condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
        };
    }

    @Expression(name = "null", description = "Checks if an object is not null")
    public static boolean Null(Object obj) {
        boolean result = obj == null;
        log.atDebug().log("NULL condition result: {}", result);

        return result;
    }

}
