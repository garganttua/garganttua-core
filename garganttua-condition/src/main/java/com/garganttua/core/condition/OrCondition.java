package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrCondition implements ICondition {

    private Set<ICondition> conditions;

    public OrCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering OrCondition constructor with {} conditions",
                conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting OrCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for OrCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.atDebug().log("Evaluating OR condition - at least one of {} conditions must be true", conditions.size());
                boolean result = or(conditions);
                log.atDebug().log("OR condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "or", description = "Logical OR of multiple conditions")
    public static boolean or(Set<ICondition> conditions) {
        int conditionIndex = 0;
        for (ICondition c : conditions) {
            boolean conditionResult = c.fullEvaluate();
            log.atDebug().log("Condition {} result: {}", conditionIndex++, conditionResult);
            if (conditionResult) {
                log.atDebug().log("OR condition evaluation complete: true (short-circuited)");
                log.atTrace().log("Exiting evaluate() with result: true");
                return conditionResult;
            }
        }
        return false;
    }

}
