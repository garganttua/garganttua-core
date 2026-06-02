package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.EqualsCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builds an {@link EqualsCondition} that compares the values produced by two
 * suppliers of the same type for equality.
 *
 * @param <T> the type of the values being compared
 */
@Reflected
public class EqualsConditionBuilder<T> implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(EqualsConditionBuilder.class);

    private ISupplierBuilder<T, ISupplier<T>> supplier1;
    private ISupplierBuilder<T, ISupplier<T>> supplier2;

    /**
     * Creates a builder comparing the values of the two suppliers for equality.
     *
     * @param supplier1 the first value supplier builder; must be non-null
     * @param supplier2 the second value supplier builder; must be non-null
     * @throws DslException if the two suppliers do not supply the same type
     */
    public EqualsConditionBuilder(ISupplierBuilder<T, ISupplier<T>> supplier1,
            ISupplierBuilder<T, ISupplier<T>> supplier2) {
        log.trace("Entering EqualsConditionBuilder constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier builder 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier builder 2 cannot be null");
        if (!this.supplier1.getSuppliedClass().equals(this.supplier2.getSuppliedClass())) {
            log.error("Type mismatch: {} VS {}",
                this.supplier1.getSuppliedClass().getSimpleName(),
                this.supplier2.getSuppliedClass().getSimpleName());
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedClass().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedClass().getSimpleName());
        }
        log.trace("Exiting EqualsConditionBuilder constructor");
    }

    /**
     * Builds the equality condition, or {@code null} when this builder is
     * contextual (deferred resolution).
     *
     * @return the composed {@link EqualsCondition}, or {@code null} if contextual
     * @throws DslException if either supplier fails to build
     */
    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for EqualsConditionBuilder");
        log.debug("Building EQUALS condition from supplier builders");

        ICondition condition = null;
        if (!isContextual())
            condition = new EqualsCondition<>(supplier1.build(), supplier2.build());

        log.debug("EQUALS condition built successfully");
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
