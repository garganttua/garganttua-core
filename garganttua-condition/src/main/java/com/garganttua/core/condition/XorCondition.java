package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

import com.garganttua.core.reflection.annotations.Reflected;
/**
 * Logical XOR {@link ICondition}: holds when an odd number of the wrapped conditions hold.
 */
@Reflected(queryAllDeclaredMethods = true)
public class XorCondition implements ICondition {
    private static final Logger log = Logger.getLogger(XorCondition.class);

    private Set<ICondition> conditions;

    /**
     * Creates an XOR aggregate over the supplied conditions.
     *
     * @param conditions the conditions to XOR together; must not be {@code null}
     * @throws NullPointerException if {@code conditions} is {@code null}
     */
    public XorCondition(Set<ICondition> conditions) {
        log.trace("Entering XorCondition constructor with {} conditions",
                conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.trace("Exiting XorCondition constructor");
    }

    /**
     * Builds a lazy supplier that evaluates the logical XOR of the wrapped conditions.
     *
     * @return a {@link Boolean} supplier yielding {@code true} when an odd number of conditions hold
     * @throws ConditionException if the condition cannot be evaluated
     */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for XorCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating XOR condition - odd number of conditions must be true");
                Boolean result = or(conditions);
                log.debug("XOR condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    /**
     * Evaluates the logical XOR of the given conditions by toggling a flag for each {@code true}.
     *
     * @param conditions the conditions to XOR together
     * @return {@code true} if an odd number of conditions evaluate to {@code true}, {@code false} otherwise
     */
    @Expression(name = "xor", description = "Logical XOR of multiple conditions")
    public static Boolean or(Set<ICondition> conditions) {
        boolean result = false;
        int conditionIndex = 0;

        for (ICondition condition : conditions) {
            boolean conditionResult = condition.fullEvaluate();
            log.debug("Condition {} result: {}", conditionIndex++, conditionResult);
            if (conditionResult) {
                result = !result;
            }
        }
        return result;
    }

}
