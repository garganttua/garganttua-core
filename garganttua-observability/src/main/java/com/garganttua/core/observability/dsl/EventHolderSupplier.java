package com.garganttua.core.observability.dsl;

import java.lang.reflect.Type;
import java.util.Optional;

import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Mutable {@link ISupplier} used as a bridge between the {@code garganttua-condition}
 * DSL (which takes its input via a supplier at construction time) and an event
 * stream (where the event under test changes for every delivery).
 *
 * <p>One instance is created per observer subscription and shared between the
 * filter's compiled condition and the framework's delivery loop. Before each
 * delivery, the framework calls {@link #setCurrent(ObservableEvent)}; the
 * condition then reads it through {@link #supply()}.
 *
 * <p>The current event field is {@code volatile} rather than {@link ThreadLocal}
 * because observers are invoked on the producer thread — i.e. the same thread
 * that called {@link #setCurrent(ObservableEvent)} right before the condition
 * evaluation. Cross-thread visibility is therefore not required, but the
 * {@code volatile} keeps any incidental reordering safe.
 *
 * @since 2.0.0-ALPHA02
 */
public final class EventHolderSupplier implements ISupplier<ObservableEvent> {

    private volatile ObservableEvent current;

    /**
     * Bind the event that subsequent {@link #supply()} calls will return.
     * Called by the framework right before each filter evaluation.
     */
    public void setCurrent(ObservableEvent event) {
        this.current = event;
    }

    @Override
    public Optional<ObservableEvent> supply() throws SupplyException {
        return Optional.ofNullable(this.current);
    }

    @Override
    public Type getSuppliedType() {
        return ObservableEvent.class;
    }

    @Override
    public IClass<ObservableEvent> getSuppliedClass() {
        return IClass.getClass(ObservableEvent.class);
    }
}
