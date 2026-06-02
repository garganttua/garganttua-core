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

/**
 * Builds a {@link CustomCondition} that applies an arbitrary {@link Predicate}
 * directly to a supplied value.
 *
 * @param <T> the type of the value tested by the predicate
 */
@Reflected
public class CustomConditionBuilder<T> implements IConditionBuilder {
    private static final Logger log = Logger.getLogger(CustomConditionBuilder.class);

    private final ISupplierBuilder<T, ? extends ISupplier<T>> builder;
    private final Predicate<T> predicate;

    /**
     * Creates a builder pairing a value supplier with a predicate.
     *
     * @param builder   the value supplier builder; must be non-null
     * @param predicate the predicate to apply to the supplied value; must be non-null
     */
    public CustomConditionBuilder(ISupplierBuilder<T, ? extends ISupplier<T>> builder,
            Predicate<T> predicate) {
        log.trace("Entering CustomConditionBuilder constructor");
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        log.trace("Exiting CustomConditionBuilder constructor");
    }

    /**
     * Builds the custom condition, or {@code null} when this builder is
     * contextual (deferred resolution).
     *
     * @return the composed {@link CustomCondition}, or {@code null} if contextual
     * @throws DslException if the value supplier fails to build
     */
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

    /**
     * @return {@code true} if the value supplier builder is contextual
     */
    @Override
    public boolean isContextual() {
        return this.builder.isContextual();
    }

}
