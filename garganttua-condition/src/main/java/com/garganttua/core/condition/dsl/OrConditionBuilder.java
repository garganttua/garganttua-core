package com.garganttua.core.condition.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ConditionException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.OrCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class OrConditionBuilder implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(OrConditionBuilder.class);

    private IConditionBuilder[] conditions;

    public OrConditionBuilder(IConditionBuilder[] conditions) throws ConditionException{
        log.trace("Entering OrConditionBuilder constructor with {} conditions", conditions != null ? conditions.length : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if( this.conditions.length < 1 ){
            log.error("No condition provided to OrConditionBuilder");
            throw new ConditionException("No condition provided");
        }
        log.trace("Exiting OrConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for OrConditionBuilder");
        log.debug("Building OR condition from {} condition builders", conditions.length);

        ICondition condition = null;
        if (!isContextual())
            condition = new OrCondition(Arrays.stream(this.conditions).map(b -> b.build()).collect(Collectors.toSet()));

        log.debug("OR condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    @Override
    public boolean isContextual() {
        return Arrays.stream(this.conditions).anyMatch(IConditionBuilder::isContextual);
    }

}
