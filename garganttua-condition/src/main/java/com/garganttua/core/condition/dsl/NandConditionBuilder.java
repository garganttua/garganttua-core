package com.garganttua.core.condition.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ConditionException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NandCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builds a {@link NandCondition} that is satisfied unless every supplied
 * sub-condition evaluates to {@code true} (the negation of AND).
 */
@Reflected
public class NandConditionBuilder implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(NandConditionBuilder.class);

    private IConditionBuilder[] conditions;

    /**
     * Creates a builder over the operand conditions to be NAND-combined.
     *
     * @param conditions the sub-condition builders; must be non-null and non-empty
     * @throws ConditionException if no condition is provided
     */
    public NandConditionBuilder(IConditionBuilder[] conditions) throws ConditionException{
        log.trace("Entering NandConditionBuilder constructor with {} conditions", conditions != null ? conditions.length : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if( this.conditions.length < 1 ){
            log.error("No condition provided to NandConditionBuilder");
            throw new ConditionException("No condition provided");
        }
        log.trace("Exiting NandConditionBuilder constructor");
    }

    /**
     * Builds the NAND condition, or {@code null} when this builder is contextual
     * (deferred resolution).
     *
     * @return the composed {@link NandCondition}, or {@code null} if contextual
     * @throws DslException if any sub-condition fails to build
     */
    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for NandConditionBuilder");
        log.debug("Building NAND condition from {} condition builders", conditions.length);

        ICondition condition = null;
        if (!isContextual())
            condition = new NandCondition(Arrays.stream(this.conditions).map(b -> b.build()).collect(Collectors.toSet()));

        log.debug("NAND condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    /**
     * @return {@code true} if any sub-condition builder is contextual
     */
    @Override
    public boolean isContextual() {
        return Arrays.stream(this.conditions).anyMatch(IConditionBuilder::isContextual);
    }

}
