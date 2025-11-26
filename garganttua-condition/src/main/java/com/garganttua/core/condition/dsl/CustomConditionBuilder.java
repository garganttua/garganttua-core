package com.garganttua.core.condition.dsl;

import java.util.Objects;
import java.util.function.Predicate;

import com.garganttua.core.condition.CustomCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

public class CustomConditionBuilder<T> implements IConditionBuilder {

    private final IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder;
    private final Predicate<T> predicate;

    public CustomConditionBuilder(IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Predicate<T> predicate) {
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
    }

    @Override
    public ICondition build() throws DslException {
        return new CustomCondition<>(this.builder.build(), this.predicate);
    }

}
