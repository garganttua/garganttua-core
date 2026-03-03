package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotNullCondition implements ICondition {

    private ISupplier<?> supplier;

    public NotNullCondition(ISupplier<?> supplier) {
        log.atTrace().log("Entering NotNullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier cannot be null");
        log.atTrace().log("Exiting NotNullCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NotNullCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.atDebug().log("Evaluating NOT NULL condition - negation of NULL condition");
                boolean result = notNull(supplier.supply().orElse(null));
                log.atDebug().log("NOT NULL condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "notNull", description = "Checks if an object is not null")
    public static boolean notNull(Object obj) {
        boolean nullResult = obj == null;
        log.atDebug().log("NULL condition result: {}", nullResult);

        boolean result = !nullResult;
        return result;
    }

}
