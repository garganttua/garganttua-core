package com.garganttua.core.condition;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.expression.annotations.ExpressionNode;
import com.garganttua.core.supply.FixedSupplier;
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

    /*
     * TODO: this method do a full evaluation, find a way to delegate the effective
     * evaluation within the returned supplier
     */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NandCondition with {} conditions", conditions.size());
        log.atDebug().log("Evaluating NAND condition - negation of AND condition");

        Boolean result = nand(this.conditions);
        log.atInfo().log("NAND condition evaluation complete: {}", result);
        log.atTrace().log("Exiting evaluate() with result: {}", result);
        return new FixedSupplier<Boolean>(result);
    }

    @ExpressionNode(name = "nand", description = "Logical AND of multiple conditions")
    public static Boolean nand(Set<ICondition> conditions) {
        boolean andResult = new AndCondition(conditions).fullEvaluate();
        log.atDebug().log("AND condition result: {}", andResult);

        boolean result = !andResult;
        return result;
    }

}
