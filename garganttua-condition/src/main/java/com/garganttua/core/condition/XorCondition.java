package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

public class XorCondition implements ICondition {

    private Set<ICondition> conditions;

    public XorCondition(Set<ICondition> conditions) {
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        boolean result = false;

        for (ICondition condition : conditions) {
            if (condition.evaluate()) {
                result = !result;
            }
        }

        return result;
    }

}
