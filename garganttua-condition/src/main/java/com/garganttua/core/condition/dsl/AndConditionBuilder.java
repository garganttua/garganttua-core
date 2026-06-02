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

/**
 * Builds an {@link AndCondition} that is satisfied only when every supplied
 * sub-condition evaluates to {@code true}.
 */
@Reflected
public class AndConditionBuilder implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(AndConditionBuilder.class);

    private IConditionBuilder[] conditions;

    /**
     * Creates a builder over the operand conditions to be AND-combined.
     *
     * @param conditions the sub-condition builders; must be non-null and non-empty
     * @throws ConditionException if no condition is provided
     */
    public AndConditionBuilder(IConditionBuilder[] conditions) throws ConditionException{
        log.trace("Entering AndConditionBuilder constructor with {} conditions", conditions != null ? conditions.length : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        if( this.conditions.length < 1 ){
            log.error("No condition provided to AndConditionBuilder");
            throw new ConditionException("No condition provided");
        }
        log.trace("Exiting AndConditionBuilder constructor");
    }

    /**
     * Builds the AND condition, or {@code null} when this builder is contextual
     * (deferred resolution).
     *
     * @return the composed {@link AndCondition}, or {@code null} if contextual
     * @throws DslException if any sub-condition fails to build
     */
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

    /**
     * @return {@code true} if any sub-condition builder is contextual
     */
    @Override
    public boolean isContextual() {
        return Arrays.stream(this.conditions).anyMatch(IConditionBuilder::isContextual);
    }

}
