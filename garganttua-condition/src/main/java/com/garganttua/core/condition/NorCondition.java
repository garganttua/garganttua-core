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
public class NorCondition implements ICondition {

    private Set<ICondition> conditions;

    public NorCondition(Set<ICondition> conditions) {
        log.atTrace().log("Entering NorCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.atTrace().log("Exiting NorCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.atTrace().log("Entering evaluate() for NorCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.atDebug().log("Evaluating NOR condition - negation of OR condition");
                boolean result = nor(conditions);
                log.atDebug().log("NOR condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    @Expression(name = "nor", description = "Logical NOR of multiple conditions")
    public static boolean nor(Set<ICondition> conditions) {
        boolean orResult = new OrCondition(conditions).fullEvaluate();
        log.atDebug().log("OR condition result: {}", orResult);

        boolean result = !orResult;
        return result;
    }

}
