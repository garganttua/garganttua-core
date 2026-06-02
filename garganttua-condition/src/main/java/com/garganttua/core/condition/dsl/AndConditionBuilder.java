package com.garganttua.core.condition.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.AndCondition;
import com.garganttua.core.condition.ConditionException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class AndConditionBuilder implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(AndConditionBuilder.class);

    private IConditionBuilder[] conditions;

    public AndConditionBuilder(IConditionBuilder[] conditions) throws ConditionException{
        log.trace("Entering AndConditionBuilder constructor with {} conditions", conditions != null ? conditions.length : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if( this.conditions.length < 1 ){
            log.error("No condition provided to AndConditionBuilder");
            throw new ConditionException("No condition provided");
        }
        log.trace("Exiting AndConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for AndConditionBuilder");
        log.debug("Building AND condition from {} condition builders", conditions.length);

        ICondition condition = null;
        if( !isContextual() )
            condition = new AndCondition(Arrays.stream(this.conditions).map(b -> b.build()).collect(Collectors.toSet()));

        log.debug("AND condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    @Override
    public boolean isContextual() {
        return Arrays.stream(this.conditions).anyMatch(IConditionBuilder::isContextual);
    }

}
