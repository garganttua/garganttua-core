package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

public class NorCondition implements ICondition {

    private Set<ICondition> conditions;

    public NorCondition(Set<ICondition> conditions) {
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        return !new OrCondition(conditions).evaluate();
    }

}
