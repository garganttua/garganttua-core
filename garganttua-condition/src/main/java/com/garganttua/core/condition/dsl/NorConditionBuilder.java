package com.garganttua.core.condition.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ConditionException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NorCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class NorConditionBuilder implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(NorConditionBuilder.class);

    private IConditionBuilder[] conditions;

    public NorConditionBuilder(IConditionBuilder[] conditions) throws ConditionException{
        log.trace("Entering NorConditionBuilder constructor with {} conditions", conditions != null ? conditions.length : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if( this.conditions.length < 1 ){
            log.error("No condition provided to NorConditionBuilder");
            throw new ConditionException("No condition provided");
        }
        log.trace("Exiting NorConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for NorConditionBuilder");
        log.debug("Building NOR condition from {} condition builders", conditions.length);

        ICondition condition = null;
        if (!isContextual())
            condition = new NorCondition(Arrays.stream(this.conditions).map(b -> b.build()).collect(Collectors.toSet()));

        log.debug("NOR condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    @Override
    public boolean isContextual() {
        return Arrays.stream(this.conditions).anyMatch(IConditionBuilder::isContextual);
    }

}
