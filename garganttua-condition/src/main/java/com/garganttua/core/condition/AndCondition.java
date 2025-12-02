package com.garganttua.core.condition;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AndCondition implements ICondition {

    private Set<ICondition> conditions;

    public AndCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering AndCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting AndCondition constructor");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for AndCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating AND condition - all {} conditions must be true", conditions.size());

        List<Boolean> results = conditions.stream().map(c -> c.evaluate()).toList();
        log.atDebug().log("Individual condition results: {}", results);

        boolean result = true;
        for (Boolean b : results) {
            result &= b;
        }

        log.atInfo().log("AND condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return result;
    }

}
