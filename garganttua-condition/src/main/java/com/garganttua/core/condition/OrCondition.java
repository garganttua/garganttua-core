package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrCondition implements ICondition {

    private Set<ICondition> conditions;

    public OrCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering OrCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting OrCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for OrCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating OR condition - at least one of {} conditions must be true", conditions.size());

        int conditionIndex = 0;
        for (ICondition c : conditions) {
            boolean conditionResult = c.evaluate();
            log.atDebug().log("Condition {} result: {}", conditionIndex++, conditionResult);
            if (conditionResult) {
                log.atInfo().log("OR condition evaluation complete: true (short-circuited)");
                log.atTrace().log("Exiting evaluate() with result: true");
                return true;
            }
        }

        log.atInfo().log("OR condition evaluation complete: false");
        log.atTrace().log("Exiting evaluate() with result: false");
        return false;
    }

}
