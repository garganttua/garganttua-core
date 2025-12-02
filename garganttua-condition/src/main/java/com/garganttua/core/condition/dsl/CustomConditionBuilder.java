package com.garganttua.core.condition.dsl;

import java.util.Objects;
import java.util.function.Predicate;

import com.garganttua.core.condition.CustomCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomConditionBuilder<T> implements IConditionBuilder {

    private final IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder;
    private final Predicate<T> predicate;

    public CustomConditionBuilder(IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Predicate<T> predicate) {
        log.atTrace().log("Entering CustomConditionBuilder constructor");
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.atTrace().log("Exiting CustomConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.atTrace().log("Entering build() for CustomConditionBuilder");
        log.atDebug().log("Building CUSTOM condition from supplier builder and predicate");

        ICondition condition = new CustomCondition<>(this.builder.build(), this.predicate);

        log.atDebug().log("CUSTOM condition built successfully");
        log.atTrace().log("Exiting build()");
        return condition;
    }

}
