package com.garganttua.core.observability;

import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle returned by {@code ObservabilityBuilder.build()} that owns a set of
 * observer wrappers plus zero-to-many active subscriptions on
 * {@link IObservable} sources.
 *
 * <p>Sources are not declared at build time — engine builders that produce
 * observable engines declare a dependency on the observability builder and,
 * once they finish building, call {@link #attachSource(IObservable)} on this
 * binding. Each call subscribes every wrapper to the new source and records
 * the resulting {@code (source, wrapper)} pair so {@link #close()} can
 * detach them later.
 *
 * <p>Closing the binding is idempotent and detaches every observer it
 * registered. Subsequent {@link #attachSource(IObservable)} calls become
 * no-ops after close.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ObservabilityBinding implements AutoCloseable, IBootstrapSummaryContributor {
    private static final Logger log = Logger.getLogger(ObservabilityBinding.class);

    /**
     * A single {@code (source, wrapper)} subscription tracked so {@link #close()}
     * can detach in the reverse order it attached.
     */
    public record Registration(IObservable source,
            IObserver<ObservableEvent> wrapper) {
        public Registration {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(wrapper, "wrapper");
        }
    }

    private final List<IObserver<ObservableEvent>> wrappers;
    private final List<Registration> registrations = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a binding owning the given observer wrappers. The list is
     * defensively copied; sources are attached later via
     * {@link #attachSource(IObservable)}.
     *
     * @param wrappers the observer wrappers produced by the parent builder
     */
    public ObservabilityBinding(List<IObserver<ObservableEvent>> wrappers) {
        this.wrappers = List.copyOf(Objects.requireNonNull(wrappers, "wrappers"));
    }

    /**
     * Subscribe every observer wrapper owned by this binding to the given
     * source. Engine builders that declared the observability builder as a
     * dependency call this from their {@code doBuild()} hook with the freshly
     * built engine instance.
     *
     * <p>If the binding has already been closed, the call is a silent no-op.
     * If the binding has no wrappers (e.g. the user built an empty
     * {@code ObservabilityBuilder}), it is a silent no-op too.
     */
    public void attachSource(IObservable source) {
        Objects.requireNonNull(source, "source");
        if (this.closed.get() || this.wrappers.isEmpty()) {
            return;
        }
        for (IObserver<ObservableEvent> w : this.wrappers) {
            source.addObserver(w);
            this.registrations.add(new Registration(source, w));
        }
        log.trace("Attached {} wrapper(s) to source {}",
                this.wrappers.size(), source.getClass().getSimpleName());
    }

    /**
     * @return the number of active (source, wrapper) registrations.
     */
    public int count() {
        return this.registrations.size();
    }

    /**
     * @return the number of observer wrappers held by this binding (one per
     *         {@code .observer(...)} call on the parent builder).
     */
    public int wrapperCount() {
        return this.wrappers.size();
    }

    /**
     * @return whether {@link #close()} has been invoked successfully.
     */
    public boolean isClosed() {
        return this.closed.get();
    }

    /**
     * Detaches every observer wrapper from its source. Exceptions thrown by
     * individual {@code removeObserver} calls are caught and logged so a
     * single broken observable cannot prevent the others from being detached.
     */
    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        List<Throwable> failures = new ArrayList<>();
        for (Registration r : this.registrations) {
            try {
                r.source().removeObserver(r.wrapper());
            } catch (RuntimeException e) {
                log.warn("Failed to detach observer from {}: {}",
                        r.source().getClass().getSimpleName(), e.getMessage());
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            log.debug("{} observer detach(es) failed during close", failures.size());
        }
    }

    // -- IBootstrapSummaryContributor ---------------------------------------

    @Override
    public String getSummaryCategory() {
        return "Observability";
    }

    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Observers", String.valueOf(this.wrappers.size()));
        items.put("Active subscriptions", String.valueOf(this.registrations.size()));
        Set<String> distinctSources = new HashSet<>();
        for (Registration r : this.registrations) {
            distinctSources.add(r.source().getClass().getSimpleName());
        }
        items.put("Attached sources", String.valueOf(distinctSources.size()));
        items.put("Status", this.closed.get() ? "closed" : "active");
        return items;
    }
}
