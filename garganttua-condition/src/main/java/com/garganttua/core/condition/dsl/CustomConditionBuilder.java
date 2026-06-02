package com.garganttua.core.condition.dsl;

import java.util.Objects;
import java.util.function.Predicate;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.CustomCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class CustomConditionBuilder<T> implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(CustomConditionBuilder.class);

    private final ISupplierBuilder<T, ? extends ISupplier<T>> builder;
    private final Predicate<T> predicate;

    public CustomConditionBuilder(ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Predicate<T> predicate) {
        log.trace("Entering CustomConditionBuilder constructor");
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.trace("Exiting CustomConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for CustomConditionBuilder");
        log.debug("Building CUSTOM condition from supplier builder and predicate");

        ICondition condition = null;
        if (!isContextual())
            condition = new CustomCondition<>(this.builder.build(), this.predicate);

        log.debug("CUSTOM condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    @Override
    public boolean isContextual() {
        return this.builder.isContextual();
    }

}
