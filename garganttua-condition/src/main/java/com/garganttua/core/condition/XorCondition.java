package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Set;

import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.supply.FixedSupplier;
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

    /*
     * TODO: this method do a full evaluation, find a way to delegate the effective
     * evaluation within the returned supplier
     */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for XorCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating XOR condition - odd number of conditions must be true");

        Boolean result = or(this.conditions);

        log.atDebug().log("XOR condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
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
