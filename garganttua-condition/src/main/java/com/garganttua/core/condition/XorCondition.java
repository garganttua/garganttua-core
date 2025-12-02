package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XorCondition implements ICondition {

    private Set<ICondition> conditions;

    public XorCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering XorCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting XorCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for XorCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating XOR condition - odd number of conditions must be true");

        boolean result = false;
        int trueCount = 0;
        int conditionIndex = 0;

        for (ICondition condition : conditions) {
            boolean conditionResult = condition.evaluate();
            log.atDebug().log("Condition {} result: {}", conditionIndex++, conditionResult);
            if (conditionResult) {
                result = !result;
                trueCount++;
            }
        }

        log.atDebug().log("Total true conditions: {}", trueCount);
        log.atInfo().log("XOR condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }

}
