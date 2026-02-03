package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NorCondition implements ICondition {

    private Set<ICondition> conditions;

    public NorCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering NorCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting NorCondition constructor");
    }

    /*
        TODO: this method do a full evaluation, find a way to delegate the effective evaluation within the returned supplier
    */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NorCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating NOR condition - negation of OR condition");

        boolean result = nor(this.conditions);
        log.atDebug().log("NOR condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
    }

    @Expression(name = "nor", description = "Logical NOR of multiple conditions")
    public static boolean nor(Set<ICondition> conditions) {
        boolean orResult = new OrCondition(conditions).fullEvaluate();
        log.atDebug().log("OR condition result: {}", orResult);

        boolean result = !orResult;
        return result;
    }

}
