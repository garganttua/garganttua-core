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
 * Logical OR {@link ICondition}: holds when at least one of the wrapped conditions holds.
 *
 * <p>Evaluation short-circuits on the first condition that evaluates to {@code true}.</p>
 */
@Reflected(queryAllDeclaredMethods = true)
public class OrCondition implements ICondition {
    private static final Logger log = Logger.getLogger(OrCondition.class);

    private Set<ICondition> conditions;

    /**
     * Creates an OR aggregate over the supplied conditions.
     *
     * @param conditions the conditions to OR together; must not be {@code null}
     * @throws NullPointerException if {@code conditions} is {@code null}
     */
    public OrCondition(Set<ICondition> conditions) {
        log.trace("Entering OrCondition constructor with {} conditions",
                conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.trace("Exiting OrCondition constructor");
    }

    /**
     * Builds a lazy supplier that evaluates the logical OR of the wrapped conditions.
     *
     * @return a {@link Boolean} supplier yielding {@code true} when any condition holds
     * @throws ConditionException if the condition cannot be evaluated
     */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for OrCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating OR condition - at least one of {} conditions must be true", conditions.size());
                boolean result = or(conditions);
                log.debug("OR condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    /**
     * Evaluates the logical OR of the given conditions, short-circuiting on the first {@code true}.
     *
     * @param conditions the conditions to OR together
     * @return {@code true} if at least one condition evaluates to {@code true}, {@code false} otherwise
     */
    @Expression(name = "or", description = "Logical OR of multiple conditions")
    public static boolean or(Set<ICondition> conditions) {
        int conditionIndex = 0;
        for (ICondition c : conditions) {
            boolean conditionResult = c.fullEvaluate();
            log.debug("Condition {} result: {}", conditionIndex++, conditionResult);
            if (conditionResult) {
                log.debug("OR condition evaluation complete: true (short-circuited)");
                log.trace("Exiting evaluate() with result: true");
                return conditionResult;
            }
        }
        return false;
    }

}
