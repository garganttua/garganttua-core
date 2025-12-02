package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NotNullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotNullConditionBuilder<T> implements IConditionBuilder {

    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier;

    public NotNullConditionBuilder(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        log.atTrace().log("Entering NotNullConditionBuilder constructor");
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
        log.atTrace().log("Exiting NotNullConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.atTrace().log("Entering build() for NotNullConditionBuilder");
        log.atDebug().log("Building NOT NULL condition from supplier builder");

        ICondition condition = new NotNullCondition(this.supplier.build());

        log.atDebug().log("NOT NULL condition built successfully");
        log.atTrace().log("Exiting build()");
        return condition;
    }

}
