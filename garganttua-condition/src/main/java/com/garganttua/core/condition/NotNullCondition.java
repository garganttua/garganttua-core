package com.garganttua.core.condition;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

import com.garganttua.core.reflection.annotations.Reflected;
/**
 * {@link ICondition} that holds when the value produced by its supplier is not {@code null}.
 *
 * <p>The check is Optional-aware: an empty {@link Optional} is treated as a missing
 * value (i.e. {@code null}), so it does <em>not</em> satisfy the condition.</p>
 */
@Reflected(queryAllDeclaredMethods = true)
public class NotNullCondition implements ICondition {
    private static final Logger log = Logger.getLogger(NotNullCondition.class);

    private ISupplier<?> supplier;

    /**
     * Creates a condition checking that the value supplied by {@code supplier} is not {@code null}.
     *
     * @param supplier the supplier whose value is inspected; must not be {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public NotNullCondition(ISupplier<?> supplier) {
        log.trace("Entering NotNullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier cannot be null");
        log.trace("Exiting NotNullCondition constructor");
    }

    /**
     * Builds a lazy supplier that evaluates the not-null check against the wrapped supplier's value.
     *
     * @return a {@link Boolean} supplier yielding {@code true} when the value is not {@code null}
     * @throws ConditionException if the condition cannot be evaluated
     */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for NotNullCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating NOT NULL condition - negation of NULL condition");
                boolean result = notNull(supplier.supply().orElse(null));
                log.debug("NOT NULL condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    /**
     * Tests whether {@code obj} represents a present value.
     *
     * <p>An empty {@link Optional} counts as {@code null} (no value), whereas a present
     * {@link Optional} counts as not-null; any other non-{@code null} reference is not-null.</p>
     *
     * @param obj the value to inspect (may be {@code null} or an {@link Optional})
     * @return {@code true} when {@code obj} holds a value, {@code false} otherwise
     */
    @Expression(name = "notNull", description = "Checks if an object is not null; an empty Optional counts as null")
    public static boolean notNull(Object obj) {
        // Optional-aware: an empty Optional means "no value", so it must read as
        // null here. Without this, guards built on suppliers that return
        // Optional.ofNullable(...) (e.g. request-arg lookups) are never skipped
        // because Optional.empty() is itself a non-null reference.
        boolean result = obj instanceof Optional<?> opt ? opt.isPresent() : obj != null;
        log.debug("NOT NULL condition result: {}", result);
        return result;
    }

}
