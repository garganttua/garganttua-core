package com.garganttua.core.condition.dsl;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.condition.CustomExtractedCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomExtractedConditionBuilder<T, R> implements IConditionBuilder {

    private final IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder;
    private final Function<T, R> extractor;
    private final Predicate<R> predicate;

    public CustomExtractedConditionBuilder(IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.atTrace().log("Entering CustomExtractedConditionBuilder constructor");
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.atTrace().log("Exiting CustomExtractedConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.atTrace().log("Entering build() for CustomExtractedConditionBuilder");
        log.atDebug().log("Building CUSTOM EXTRACTED condition from supplier builder, extractor, and predicate");

        ICondition condition = new CustomExtractedCondition<>(this.builder.build(), this.extractor, this.predicate);

        log.atDebug().log("CUSTOM EXTRACTED condition built successfully");
        log.atTrace().log("Exiting build()");
        return condition;
    }

}
