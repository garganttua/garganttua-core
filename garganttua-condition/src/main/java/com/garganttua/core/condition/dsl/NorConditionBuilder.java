package com.garganttua.core.condition.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.condition.ConditionException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NorCondition;
import com.garganttua.core.dsl.DslException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NorConditionBuilder implements IConditionBuilder {

    private IConditionBuilder[] conditions;

    public NorConditionBuilder(IConditionBuilder[] conditions) throws ConditionException{
        log.atTrace().log("Entering NorConditionBuilder constructor with {} conditions", conditions != null ? conditions.length : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if( this.conditions.length < 1 ){
            log.atError().log("No condition provided to NorConditionBuilder");
            throw new ConditionException("No condition provided");
        }
        log.atTrace().log("Exiting NorConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.atTrace().log("Entering build() for NorConditionBuilder");
        log.atDebug().log("Building NOR condition from {} condition builders", conditions.length);

        ICondition condition = new NorCondition(Arrays.stream(this.conditions).map(b -> b.build()).collect(Collectors.toSet()));

        log.atDebug().log("NOR condition built successfully");
        log.atTrace().log("Exiting build()");
        return condition;
    }

}
