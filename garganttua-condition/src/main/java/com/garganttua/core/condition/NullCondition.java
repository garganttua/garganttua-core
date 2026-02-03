package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.expression.annotations.Expression;
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

        Boolean result = Null(this.supplier.supply().orElse(null));

        log.atDebug().log("NULL condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
    }

    @Expression(name = "null", description = "Checks if an object is not null")
    public static boolean Null(Object obj) {
        boolean result = obj == null;
        log.atDebug().log("NULL condition result: {}", result);

        return result;
    }

}
