package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class NullConditionBuilder<T> implements IConditionBuilder {
    private static final IDiagnostic log = Diagnostics.of(NullConditionBuilder.class);

    private ISupplierBuilder<T, ISupplier<T>> supplier;

    public NullConditionBuilder(ISupplierBuilder<T, ISupplier<T>> supplier) {
        log.trace("Entering NullConditionBuilder constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.trace("Exiting NullConditionBuilder constructor");
    }

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

    @Override
    public boolean isContextual() {
        return this.supplier.isContextual();
    }

}
