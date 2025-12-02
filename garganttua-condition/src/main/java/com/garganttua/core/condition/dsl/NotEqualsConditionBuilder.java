package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NotEqualsCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotEqualsConditionBuilder<T> implements IConditionBuilder {

    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1;
    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2;

    public NotEqualsConditionBuilder(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1,
            IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2) {
        log.atTrace().log("Entering NotEqualsConditionBuilder constructor");
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 builder cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 builder cannot be null");
        if (!this.supplier1.getSuppliedType().equals(this.supplier2.getSuppliedType())) {
            log.atError().log("Type mismatch: {} VS {}",
                this.supplier1.getSuppliedType().getSimpleName(),
                this.supplier2.getSuppliedType().getSimpleName());
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedType().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedType().getSimpleName());
        }
        log.atTrace().log("Exiting NotEqualsConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.atTrace().log("Entering build() for NotEqualsConditionBuilder");
        log.atDebug().log("Building NOT EQUALS condition from supplier builders");

        ICondition condition = new NotEqualsCondition<>(supplier1.build(), supplier2.build());

        log.atDebug().log("NOT EQUALS condition built successfully");
        log.atTrace().log("Exiting build()");
        return condition;
    }

}
