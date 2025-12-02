package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NandCondition implements ICondition {

    private Set<ICondition> conditions;

    public NandCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering NandCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting NandCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NandCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating NAND condition - negation of AND condition");

        boolean andResult = new AndCondition(conditions).evaluate();
        log.atDebug().log("AND condition result: {}", andResult);

        boolean result = !andResult;
        log.atInfo().log("NAND condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }

}
