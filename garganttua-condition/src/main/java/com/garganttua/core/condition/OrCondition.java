package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

public class OrCondition implements ICondition {

    private Set<ICondition> conditions;

    public OrCondition(Set<ICondition> conditions) {
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        for (ICondition c : conditions) {
            if (c.evaluate()) {
                return true;
            }
        }
        return false;
    }

}
