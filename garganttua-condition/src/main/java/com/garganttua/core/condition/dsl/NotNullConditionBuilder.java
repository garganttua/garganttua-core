package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NotNullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builds a {@link NotNullCondition} that is satisfied when the supplied value is
 * not {@code null}.
 *
 * @param <T> the type of the value being checked
 */
@Reflected
public class NotNullConditionBuilder<T> implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(NotNullConditionBuilder.class);

    private ISupplierBuilder<T, ISupplier<T>> supplier;

    /**
     * Creates a builder checking the supplied value for non-nullity.
     *
     * @param supplier the value supplier builder; must be non-null
     */
    public NotNullConditionBuilder(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.trace("Entering NotNullConditionBuilder constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.trace("Exiting NotNullConditionBuilder constructor");
    }

    /**
     * Builds the not-null-check condition, or {@code null} when this builder is
     * contextual (deferred resolution).
     *
     * @return the composed {@link NotNullCondition}, or {@code null} if contextual
     * @throws DslException if the supplier fails to build
     */
    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for NotNullConditionBuilder");
        log.debug("Building NOT NULL condition from supplier builder");

        ICondition condition = null;
        if (!isContextual())
            condition = new NotNullCondition(this.supplier.build());

        log.debug("NOT NULL condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    /**
     * @return {@code true} if the value supplier builder is contextual
     */
    @Override
    public boolean isContextual() {
        return this.supplier.isContextual();
    }

}
