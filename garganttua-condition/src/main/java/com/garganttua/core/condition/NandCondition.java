package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NandCondition implements ICondition {

    private Set<ICondition> conditions;

    public NandCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering NandCondition constructor with {} conditions",
                conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting NandCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NandCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.atDebug().log("Evaluating NAND condition - negation of AND condition");
                Boolean result = nand(conditions);
                log.atDebug().log("NAND condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
        };
    }

    @Expression(name = "nand", description = "Logical AND of multiple conditions")
    public static Boolean nand(Set<ICondition> conditions) {
        boolean andResult = new AndCondition(conditions).fullEvaluate();
        log.atDebug().log("AND condition result: {}", andResult);

        boolean result = !andResult;
        return result;
    }

}
