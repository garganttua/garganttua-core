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
 * {@link ICondition} that holds when the value produced by its supplier is {@code null}.
 *
 * <p>The check is the strict inverse of {@link NotNullCondition} and is Optional-aware:
 * an empty {@link Optional} is treated as a missing value and therefore satisfies the condition.</p>
 */
@Reflected(queryAllDeclaredMethods = true)
public class NullCondition implements ICondition {
    private static final Logger log = Logger.getLogger(NullCondition.class);

    private ISupplier<?> supplier;

    /**
     * Creates a condition checking that the value supplied by {@code supplier} is {@code null}.
     *
     * @param supplier the supplier whose value is inspected; must not be {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public NullCondition(ISupplier<?> supplier) {
        log.trace("Entering NullCondition constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.trace("Exiting NullCondition constructor");
    }

    /**
     * Builds a lazy supplier that evaluates the null check against the wrapped supplier's value.
     *
     * @return a {@link Boolean} supplier yielding {@code true} when the value is {@code null}
     * @throws ConditionException if the condition cannot be evaluated
     */
    @Override
    public ISupplier<Boolean> evaluate() throws ConditionException {
        log.trace("Entering evaluate() for NullCondition");
        return new ISupplier<Boolean>() {
            @Override
            public Optional<Boolean> supply() {
                log.debug("Evaluating NULL condition - checking if supplier returns null/empty");
                Boolean result = Null(supplier.supply().orElse(null));
                log.debug("NULL condition evaluation complete: {}", result);
                return Optional.of(result);
            }
            @Override
            public Type getSuppliedType() { return Boolean.class; }
            @Override
            public IClass<Boolean> getSuppliedClass() { return IClass.getClass(Boolean.class); }
        };
    }

    /**
     * Tests whether {@code obj} represents a missing value.
     *
     * <p>An empty {@link Optional} counts as {@code null} (no value); a present
     * {@link Optional} or any other non-{@code null} reference counts as not-null.</p>
     *
     * @param obj the value to inspect (may be {@code null} or an {@link Optional})
     * @return {@code true} when {@code obj} holds no value, {@code false} otherwise
     */
    @Expression(name = "null", description = "Checks if an object is null; an empty Optional counts as null")
    public static boolean Null(Object obj) {
        // Strict inverse of NotNullCondition.notNull — Optional-aware so that an
        // empty Optional reads as null (the value semantics, not the reference).
        boolean result = obj instanceof Optional<?> opt ? opt.isEmpty() : obj == null;
        log.debug("NULL condition result: {}", result);
        return result;
    }

}
