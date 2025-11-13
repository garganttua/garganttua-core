package com.garganttua.core.condition.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.condition.ConditionException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.OrCondition;
import com.garganttua.core.dsl.DslException;

public class OrConditionBuilder implements IConditionBuilder {

    private IConditionBuilder[] conditions;

    public OrConditionBuilder(IConditionBuilder[] conditions) throws ConditionException{
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if( this.conditions.length < 1 ){
            throw new ConditionException("No condition provided");
        }
    }

    @Override
    public ICondition build() throws DslException {
        return new OrCondition(Arrays.stream(this.conditions).map(b -> b.build()).collect(Collectors.toSet()));
    }

}
