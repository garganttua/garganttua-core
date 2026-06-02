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
 * Logical NOR condition: the negation of {@link OrCondition}, i.e. {@code true}
 * only when none of the wrapped conditions evaluate to {@code true}.
 */
@Reflected(queryAllDeclaredMethods = true)
public class NorCondition implements ICondition {
    private static final Logger log = Logger.getLogger(NorCondition.class);

    private Set<ICondition> conditions;

    /**
     * Creates a NOR condition over the given conditions.
     *
     * @param conditions the conditions to combine; must not be {@code null}
     */
    public NorCondition(Set<ICondition> conditions) {
        log.trace("Entering NorCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.trace("Exiting NorCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for NorCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating NOR condition - negation of OR condition");
                boolean result = nor(conditions);
                log.debug("NOR condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    /**
     * Logical NOR of multiple conditions: the negation of their OR.
     *
     * @param conditions the conditions to combine
     * @return {@code true} only when no condition evaluates to {@code true}
     */
    @Expression(name = "nor", description = "Logical NOR of multiple conditions")
    public static boolean nor(Set<ICondition> conditions) {
        boolean orResult = new OrCondition(conditions).fullEvaluate();
        log.debug("OR condition result: {}", orResult);

        boolean result = !orResult;
        return result;
    }

}
