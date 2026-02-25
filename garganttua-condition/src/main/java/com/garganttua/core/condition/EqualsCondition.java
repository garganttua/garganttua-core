package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.expression.annotations.Expression;
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
        log.atTrace().log("Exiting EqualsCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for EqualsCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.atDebug().log("Evaluating EQUALS condition - comparing two supplied values");
                Object val1 = supplier1.supply().orElseThrow(() -> {
                    log.atError().log("Supplier 1 supplied empty value");
                    return new ConditionException("Supplier 1 supplied empty value");
                });
                Object val2 = supplier2.supply().orElseThrow(() -> {
                    log.atError().log("Supplier 2 supplied empty value");
                    return new ConditionException("Supplier 2 supplied empty value");
                });
                boolean result = EqualsCondition.equals(val1, val2);
                log.atDebug().log("EQUALS condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
        };
    }

    @Expression(name = "equals", description = "Checks if two objects are equal")
    public static boolean equals(Object obj1, Object obj2) {
        log.atTrace().log("Entering static equals() method");
                if( obj1 == null || obj2 == null ) {
            return false;
        }
        if (!obj1.getClass().equals(obj2.getClass())) {
            log.atError().log("Type mismatch: {} VS {}",
                obj1.getClass().getSimpleName(),
                obj2.getClass().getSimpleName());
            return false;
        }
        boolean result = Objects.equals(obj1, obj2);
        log.atDebug().log("Equality check result for objects {} and {}: {}", obj1, obj2, result);
        log.atTrace().log("Exiting static equals() method with result: {}", result);
        return result;
    }

}
