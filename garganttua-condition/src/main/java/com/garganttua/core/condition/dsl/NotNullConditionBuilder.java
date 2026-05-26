package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NotNullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class NotNullConditionBuilder<T> implements IConditionBuilder {
    private static final IDiagnostic log = Diagnostics.of(NotNullConditionBuilder.class);

    private ISupplierBuilder<T, ISupplier<T>> supplier;

    public NotNullConditionBuilder(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.trace("Entering NotNullConditionBuilder constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.trace("Exiting NotNullConditionBuilder constructor");
    }

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

    @Override
    public boolean isContextual() {
        return this.supplier.isContextual();
    }

}
