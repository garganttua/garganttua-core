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
 * Logical NAND condition: the negation of {@link AndCondition}, i.e. {@code true}
 * unless every wrapped condition evaluates to {@code true}.
 */
@Reflected(queryAllDeclaredMethods = true)
public class NandCondition implements ICondition {
    private static final Logger log = Logger.getLogger(NandCondition.class);

    private Set<ICondition> conditions;

    /**
     * Creates a NAND condition over the given conditions.
     *
     * @param conditions the conditions to combine; must not be {@code null}
     */
    public NandCondition(Set<ICondition> conditions) {
        log.trace("Entering NandCondition constructor with {} conditions",
                conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.trace("Exiting NandCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for NandCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating NAND condition - negation of AND condition");
                Boolean result = nand(conditions);
                log.debug("NAND condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    /**
     * Logical NAND of multiple conditions: the negation of their AND.
     *
     * @param conditions the conditions to combine
     * @return {@code true} unless every condition evaluates to {@code true}
     */
    @Expression(name = "nand", description = "Logical AND of multiple conditions")
    public static Boolean nand(Set<ICondition> conditions) {
        boolean andResult = new AndCondition(conditions).fullEvaluate();
        log.debug("AND condition result: {}", andResult);

        boolean result = !andResult;
        return result;
    }

}
