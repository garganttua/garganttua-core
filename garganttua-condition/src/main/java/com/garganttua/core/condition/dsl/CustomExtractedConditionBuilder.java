package com.garganttua.core.condition.dsl;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.CustomExtractedCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builds a {@link CustomExtractedCondition} that extracts a value from the
 * supplied object via a {@link Function} and tests it with a {@link Predicate}.
 *
 * @param <T> the type of the supplied object
 * @param <R> the type of the extracted value tested by the predicate
 */
@Reflected
public class CustomExtractedConditionBuilder<T, R> implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(CustomExtractedConditionBuilder.class);

    private final ISupplierBuilder<T, ? extends ISupplier<T>> builder;
    private final Function<T, R> extractor;
    private final Predicate<R> predicate;

    /**
     * Creates a builder pairing a value supplier with an extractor and a predicate.
     *
     * @param builder   the source value supplier builder; must be non-null
     * @param extractor extracts the value to test from the supplied object; must be non-null
     * @param predicate the predicate to apply to the extracted value; must be non-null
     */
    public CustomExtractedConditionBuilder(ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Function<T, R> extractor,
            Predicate<R> predicate) {
        log.trace("Entering CustomExtractedConditionBuilder constructor");
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.extractor = Objects.requireNonNull(extractor, "Extractor cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.trace("Exiting CustomExtractedConditionBuilder constructor");
    }

    /**
     * Builds the custom extracted condition, or {@code null} when this builder
     * is contextual (deferred resolution).
     *
     * @return the composed {@link CustomExtractedCondition}, or {@code null} if contextual
     * @throws DslException if the value supplier fails to build
     */
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

    /**
     * @return {@code true} if the source value supplier builder is contextual
     */
    @Override
    public boolean isContextual() {
        return this.builder.isContextual();
    }

}
