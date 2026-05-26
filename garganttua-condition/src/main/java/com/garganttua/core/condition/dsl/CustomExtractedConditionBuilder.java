package com.garganttua.core.condition.dsl;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.condition.CustomExtractedCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class CustomExtractedConditionBuilder<T, R> implements IConditionBuilder {
    private static final IDiagnostic log = Diagnostics.of(CustomExtractedConditionBuilder.class);

    private final ISupplierBuilder<T, ? extends ISupplier<T>> builder;
    private final Function<T, R> extractor;
    private final Predicate<R> predicate;

    public CustomExtractedConditionBuilder(ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.trace("Entering CustomExtractedConditionBuilder constructor");
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.trace("Exiting CustomExtractedConditionBuilder constructor");
    }

    @Override
    public ICondition build() throws DslException {
        log.trace("Entering build() for CustomExtractedConditionBuilder");
        log.debug("Building CUSTOM EXTRACTED condition from supplier builder, extractor, and predicate");

        ICondition condition = null;
        if (!isContextual())
            condition = new CustomExtractedCondition<>(this.builder.build(), this.extractor, this.predicate);

        log.debug("CUSTOM EXTRACTED condition built successfully");
        log.trace("Exiting build()");
        return condition;
    }

    @Override
    public boolean isContextual() {
        return this.builder.isContextual();
    }

}
