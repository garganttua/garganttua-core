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
public class XorCondition implements ICondition {

    private Set<ICondition> conditions;

    public XorCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering XorCondition constructor with {} conditions",
                conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting XorCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for XorCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.atDebug().log("Evaluating XOR condition - odd number of conditions must be true");
                Boolean result = or(conditions);
                log.atDebug().log("XOR condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "xor", description = "Logical XOR of multiple conditions")
    public static Boolean or(Set<ICondition> conditions) {
        boolean result = false;
        int conditionIndex = 0;

        for (ICondition condition : conditions) {
            boolean conditionResult = condition.fullEvaluate();
            log.atDebug().log("Condition {} result: {}", conditionIndex++, conditionResult);
            if (conditionResult) {
                result = !result;
            }
        }
        return result;
    }

}
