package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullConditionBuilder<T> implements IConditionBuilder {

    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier;

    public NullConditionBuilder(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        log.atTrace().log("Entering NullConditionBuilder constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.atTrace().log("Exiting NullConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.atTrace().log("Entering build() for NullConditionBuilder");
        log.atDebug().log("Building NULL condition from supplier builder");

        ICondition condition = new NullCondition(this.supplier.build());

        log.atDebug().log("NULL condition built successfully");
        log.atTrace().log("Exiting build()");
        return condition;
    }

}
