package com.garganttua.core.condition;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AndCondition implements ICondition {

    private Set<ICondition> conditions;

    public AndCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering AndCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting AndCondition constructor");
    }

    /*
        TODO: this method do a full evaluation, find a way to delegate the effective evaluation within the returned supplier
    */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for AndCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating AND condition - all {} conditions must be true", conditions.size());

        Boolean result = and(this.conditions);

        log.atInfo().log("AND condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
    }

    @Expression(name = "and", description = "Logical AND of multiple conditions")
    public static Boolean and(Set<ICondition> conditions) {
        List<ISupplier<Boolean>> results = conditions.stream().map(c -> c.evaluate()).toList();
        log.atDebug().log("Individual condition results: {}", results);

        Boolean result = true;
        for (ISupplier<Boolean> b : results) {
            result &= b.supply().get();
        }
        return result;
    }

}
