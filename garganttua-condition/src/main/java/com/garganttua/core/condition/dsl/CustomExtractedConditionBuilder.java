package com.garganttua.core.condition.dsl;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.condition.CustomExtractedCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class CustomExtractedConditionBuilder<T, R> implements IConditionBuilder {

    private final IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder;
    private final Function<T, R> extractor;
    private final Predicate<R> predicate;

    public CustomExtractedConditionBuilder(IObjectSupplierBuilder<T, ? extends IObjectSupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
    }

    @Override
    public ICondition build() throws DslException {
        return new CustomExtractedCondition<>(this.builder.build(), this.extractor, this.predicate);
    }

}
