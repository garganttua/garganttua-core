package com.garganttua.core.condition.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.condition.ConditionException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.XorCondition;
import com.garganttua.core.dsl.DslException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XorConditionBuilder implements IConditionBuilder {

    private IConditionBuilder[] conditions;

    public XorConditionBuilder(IConditionBuilder[] conditions) throws ConditionException {
        log.atTrace().log("Entering XorConditionBuilder constructor with {} conditions", conditions != null ? conditions.length : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if (this.conditions.length < 1) {
            log.atError().log("No condition provided to XorConditionBuilder");
            throw new ConditionException("No condition provided");
        }
        log.atTrace().log("Exiting XorConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.atTrace().log("Entering build() for XorConditionBuilder");
        log.atDebug().log("Building XOR condition from {} condition builders", conditions.length);

        ICondition condition = null;
        if (!isContextual())
            condition = new XorCondition(Arrays.stream(this.conditions).map(b -> b.build()).collect(Collectors.toSet()));

        log.atDebug().log("XOR condition built successfully");
        log.atTrace().log("Exiting build()");
        return condition;
    }

    @Override
    public boolean isContextual() {
        return Arrays.stream(this.conditions).anyMatch(IConditionBuilder::isContextual);
    }

}
