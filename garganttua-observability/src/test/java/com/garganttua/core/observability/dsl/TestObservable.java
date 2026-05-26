package com.garganttua.core.observability.dsl;

import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.ObservableRegistry;

/**
 * Minimal {@link IObservable} fixture used by the DSL tests. Exposes a {@code fire}
 * method to push events to all registered observers.
 */
class TestObservable implements IObservable {

    private final ObservableRegistry registry = new ObservableRegistry();
    private final String name;

    TestObservable(String name) {
        this.name = name;
    }

    @Override
    public void addObserver(IObserver<ObservableEvent> observer) {
        this.registry.addObserver(observer);
    }

    @Override
    public void removeObserver(IObserver<ObservableEvent> observer) {
        this.registry.removeObserver(observer);
    }

    void fire(ObservableEvent event) {
        this.registry.fire(event);
    }

    int observerCount() {
        return this.registry.size();
    }

    String name() {
        return this.name;
    }
}
