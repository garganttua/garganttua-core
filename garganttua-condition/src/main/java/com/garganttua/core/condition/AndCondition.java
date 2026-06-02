package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

import jakarta.annotation.Nullable;

import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Logical AND condition: evaluates to {@code true} only when every wrapped
 * {@link ICondition} evaluates to {@code true}.
 */
@Reflected(queryAllDeclaredMethods = true)
public class AndCondition implements ICondition {
    private static final Logger log = Logger.getLogger(AndCondition.class);

    private Set<ICondition> conditions;

    /**
     * Creates an AND condition over the given conditions.
     *
     * @param conditions the conditions to combine; must not be {@code null}
     */
    public AndCondition(Set<ICondition> conditions) {
        log.trace("Entering AndCondition constructor with {} conditions", conditions != null ? conditions.size() : 0);
        this.conditions = Objects.requireNonNull(conditions, "Conditions cannot be null");
        log.trace("Exiting AndCondition constructor");
    }

    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for AndCondition with {} conditions", conditions.size());
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating AND condition - all {} conditions must be true", conditions.size());
                Boolean result = and(conditions);
                log.debug("AND condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    /**
     * Logical AND of two values coerced to boolean (see truthiness rules in the
     * private {@code toBoolean} helper).
     *
     * @param value1 first operand, may be {@code null}
     * @param value2 second operand, may be {@code null}
     * @return {@code true} when both operands are truthy
     */
    @Expression(name = "and", description = "Logical AND of two boolean values")
    public static boolean and(@Nullable Object value1, @Nullable Object value2) {
        return toBoolean(value1) && toBoolean(value2);
    }

    private static boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.doubleValue() != 0.0;
        }
        if (value instanceof String s) {
            return !s.isEmpty() && !"false".equalsIgnoreCase(s);
        }
        return true;
    }

    /**
     * Logical AND of multiple conditions: {@code true} only if all evaluate true.
     *
     * @param conditions the conditions to combine
     * @return {@code true} when every condition evaluates to {@code true}
     */
    @Expression(name = "and", description = "Logical AND of multiple conditions")
    public static Boolean and(Set<ICondition> conditions) {
        List<ISupplier<Boolean>> results = conditions.stream().map(c -> c.evaluate()).toList();
        log.debug("Individual condition results: {}", results);

        Boolean result = true;
        for (ISupplier<Boolean> b : results) {
            result &= b.supply().get();
        }
        return result;
    }

}
