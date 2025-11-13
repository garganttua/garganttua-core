package com.garganttua.core.condition;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AndCondition implements ICondition {

    private Set<ICondition> conditions;

    public AndCondition(Set<ICondition> conditions) {
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        List<Boolean> results = conditions.stream().map(c -> c.evaluate()).toList();
        boolean result = true;
        for (Boolean b : results) {
            result &= b;
        }
        return result;
    }

}
