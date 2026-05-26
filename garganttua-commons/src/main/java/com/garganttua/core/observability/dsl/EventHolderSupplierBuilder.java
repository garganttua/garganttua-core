package com.garganttua.core.observability.dsl;

import java.lang.reflect.Type;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder for {@link EventHolderSupplier}. Always returns the SAME holder
 * instance — the framework needs both the user's compiled condition and its
 * own delivery loop to share the same mutable reference.
 *
 * <p>Used by {@code ObserverBindingBuilder.when(...)} as the supplier argument
 * passed to the user's filter lambda.
 *
 * @since 2.0.0-ALPHA02
 */
public final class EventHolderSupplierBuilder
        implements ISupplierBuilder<ObservableEvent, EventHolderSupplier> {

    private final EventHolderSupplier holder;

    public EventHolderSupplierBuilder(EventHolderSupplier holder) {
        this.holder = holder;
    }

    public EventHolderSupplierBuilder() {
        this(new EventHolderSupplier());
    }

    @Override
    public EventHolderSupplier build() throws DslException {
        return this.holder;
    }

    @Override
    public IClass<ObservableEvent> getSuppliedClass() {
        return IClass.getClass(ObservableEvent.class);
    }

    @Override
    public Type getSuppliedType() {
        return ObservableEvent.class;
    }

    @Override
    public boolean isContextual() {
        return false;
    }
}
