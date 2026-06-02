package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NotEqualsCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builds a {@link NotEqualsCondition} that compares the values produced by two
 * suppliers of the same type for inequality.
 *
 * @param <T> the type of the values being compared
 */
@Reflected
public class NotEqualsConditionBuilder<T> implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(NotEqualsConditionBuilder.class);

    private ISupplierBuilder<T, ISupplier<T>> supplier1;
    private ISupplierBuilder<T, ISupplier<T>> supplier2;

    /**
     * Creates a builder comparing the values of the two suppliers for inequality.
     *
     * @param supplier1 the first value supplier builder; must be non-null
     * @param supplier2 the second value supplier builder; must be non-null
     * @throws DslException if the two suppliers do not supply the same type
     */
    public NotEqualsConditionBuilder(ISupplierBuilder<T, ISupplier<T>> supplier1,
            ISupplierBuilder<T, ISupplier<T>> supplier2) {
        log.trace("Entering NotEqualsConditionBuilder constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 builder cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 builder cannot be null");
        if (!this.supplier1.getSuppliedClass().equals(this.supplier2.getSuppliedClass())) {
            log.error("Type mismatch: {} VS {}",
                    this.supplier1.getSuppliedClass().getSimpleName(),
                    this.supplier2.getSuppliedClass().getSimpleName());
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedClass().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedClass().getSimpleName());
        }
        log.trace("Exiting NotEqualsConditionBuilder constructor");
    }

    /**
     * Builds the inequality condition, or {@code null} when this builder is
     * contextual (deferred resolution).
     *
     * @return the composed {@link NotEqualsCondition}, or {@code null} if contextual
     * @throws DslException if either supplier fails to build
     */
    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for NotEqualsConditionBuilder");
        log.debug("Building NOT EQUALS condition from supplier builders");

        ICondition condition = null;
        if (!isContextual())
            condition = new NotEqualsCondition<>(supplier1.build(), supplier2.build());

        log.debug("NOT EQUALS condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    /**
     * @return {@code true} if either value supplier builder is contextual
     */
    @Override
    public boolean isContextual() {
        return this.supplier1.isContextual() || this.supplier2.isContextual();
    }

}
