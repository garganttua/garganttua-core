package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

public class NandCondition implements ICondition {

    private Set<ICondition> conditions;

    public NandCondition(Set<ICondition> conditions) {
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        return !new AndCondition(conditions).evaluate();
    }

}
