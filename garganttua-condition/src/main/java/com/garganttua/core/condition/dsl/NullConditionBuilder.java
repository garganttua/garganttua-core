package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builds a {@link NullCondition} that is satisfied when the supplied value is
 * {@code null}.
 *
 * @param <T> the type of the value being checked
 */
@Reflected
public class NullConditionBuilder<T> implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(NullConditionBuilder.class);

    private ISupplierBuilder<T, ISupplier<T>> supplier;

    /**
     * Creates a builder checking the supplied value for nullity.
     *
     * @param supplier the value supplier builder; must be non-null
     */
    public NullConditionBuilder(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.trace("Entering NullConditionBuilder constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.trace("Exiting NullConditionBuilder constructor");
    }

    /**
     * Builds the null-check condition, or {@code null} when this builder is
     * contextual (deferred resolution).
     *
     * @return the composed {@link NullCondition}, or {@code null} if contextual
     * @throws DslException if the supplier fails to build
     */
    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for NullConditionBuilder");
        log.debug("Building NULL condition from supplier builder");

        ICondition condition = null;
        if (!isContextual())
            condition = new NullCondition(this.supplier.build());

        log.debug("NULL condition built successfully");
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
