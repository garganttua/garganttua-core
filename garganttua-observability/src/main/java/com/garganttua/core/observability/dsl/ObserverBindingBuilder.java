package com.garganttua.core.observability.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.condition.Conditions;
import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;

/**
 * Concrete {@link IObserverBindingBuilder}. Holds the filter state until the
 * root builder's {@code build()} iterates over all bindings and wires the
 * observers to their sources.
 *
 * @since 2.0.0-ALPHA02
 */
final class ObserverBindingBuilder implements IObserverBindingBuilder {

    private final IObserver<ObservableEvent> target;
    private IObservabilityBuilder up;

    /** AND-accumulated predicate filter. {@code null} = no predicate filter. */
    private Predicate<ObservableEvent> predicate;

    /** AND-accumulated condition builder. {@code null} = no condition filter. */
    private IConditionBuilder conditionBuilder;

    /** Shared between the user's condition and the framework's delivery loop. */
    private EventHolderSupplier holder;

    /** When non-null, overrides the parent builder's default observable set. */
    private List<IObservable<ObservableEvent>> overrideSources;

    ObserverBindingBuilder(IObserver<ObservableEvent> target, IObservabilityBuilder up) {
        this.target = Objects.requireNonNull(target, "observer");
        this.up = up;
    }

    @Override
    public IObserverBindingBuilder when(
            Function<EventHolderSupplierBuilder, IConditionBuilder> condition) {
        Objects.requireNonNull(condition, "condition lambda");
        if (this.holder == null) {
            this.holder = new EventHolderSupplier();
        }
        EventHolderSupplierBuilder eventSupplier = new EventHolderSupplierBuilder(this.holder);
        IConditionBuilder built = Objects.requireNonNull(condition.apply(eventSupplier),
                "condition lambda returned null");
        this.conditionBuilder = (this.conditionBuilder == null)
                ? built
                : Conditions.and(this.conditionBuilder, built);
        return this;
    }

    @Override
    public IObserverBindingBuilder where(Predicate<ObservableEvent> p) {
        Objects.requireNonNull(p, "predicate");
        this.predicate = (this.predicate == null) ? p : this.predicate.and(p);
        return this;
    }

    @Override
    @SafeVarargs
    public final IObserverBindingBuilder onlyEvents(Class<? extends ObservableEvent>... eventTypes) {
        Objects.requireNonNull(eventTypes, "eventTypes");
        if (eventTypes.length == 0) {
            return this;
        }
        Class<? extends ObservableEvent>[] types = eventTypes.clone();
        Predicate<ObservableEvent> typeFilter = e -> {
            for (Class<? extends ObservableEvent> t : types) {
                if (t.isInstance(e)) {
                    return true;
                }
            }
            return false;
        };
        return where(typeFilter);
    }

    @Override
    public IObserverBindingBuilder matchingSource(String globPattern) {
        Objects.requireNonNull(globPattern, "globPattern");
        String regex = globToRegex(globPattern);
        return where(e -> e.source() != null && e.source().matches(regex));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final IObserverBindingBuilder toObservable(IObservable<? extends ObservableEvent>... sources) {
        Objects.requireNonNull(sources, "sources");
        List<IObservable<ObservableEvent>> list = new ArrayList<>(sources.length);
        for (IObservable<? extends ObservableEvent> src : sources) {
            Objects.requireNonNull(src, "source");
            list.add((IObservable<ObservableEvent>) src);
        }
        this.overrideSources = list;
        return this;
    }

    @Override
    public IObservabilityBuilder up() {
        return this.up;
    }

    @Override
    public void setUp(IObservabilityBuilder up) {
        this.up = up;
    }

    @Override
    public Void build() throws DslException {
        // The actual registration is owned by the root ObservabilityBuilder.
        return null;
    }

    // --- Package-private accessors used by ObservabilityBuilder ---

    IObserver<ObservableEvent> target() {
        return this.target;
    }

    List<IObservable<ObservableEvent>> overrideSources() {
        return this.overrideSources;
    }

    IObserver<ObservableEvent> buildWrapper() throws DslException {
        if (this.predicate == null && this.conditionBuilder == null) {
            return this.target;
        }
        return new FilteredObserver(this.target,
                this.conditionBuilder != null ? this.conditionBuilder.build() : null,
                this.holder,
                this.predicate);
    }

    /**
     * Translates a {@code *}-only glob into a Java regex anchored on both ends.
     * Every other character is literal (escaped via Pattern.quote-like rules).
     */
    static String globToRegex(String glob) {
        StringBuilder out = new StringBuilder(glob.length() + 8);
        out.append('^');
        int i = 0;
        int n = glob.length();
        StringBuilder literal = new StringBuilder();
        while (i < n) {
            char c = glob.charAt(i);
            if (c == '*') {
                if (literal.length() > 0) {
                    out.append(java.util.regex.Pattern.quote(literal.toString()));
                    literal.setLength(0);
                }
                out.append(".*");
            } else {
                literal.append(c);
            }
            i++;
        }
        if (literal.length() > 0) {
            out.append(java.util.regex.Pattern.quote(literal.toString()));
        }
        out.append('$');
        return out.toString();
    }
}
