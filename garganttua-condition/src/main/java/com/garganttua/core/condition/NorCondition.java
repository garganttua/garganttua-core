package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NorCondition implements ICondition {

    private Set<ICondition> conditions;

    public NorCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering NorCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting NorCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NorCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating NOR condition - negation of OR condition");

        boolean orResult = new OrCondition(conditions).evaluate();
        log.atDebug().log("OR condition result: {}", orResult);

        boolean result = !orResult;
        log.atInfo().log("NOR condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }

}
