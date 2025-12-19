package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.expression.annotations.Expression;
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

        boolean result = equals(this.supplier1.supply().get(),this.supplier2.supply().get());
        log.atInfo().log("EQUALS condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
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
