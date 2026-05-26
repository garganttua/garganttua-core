package com.garganttua.core.observability;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle returned by {@code ObservabilityBuilder.build()} that owns a set of
 * observer registrations across one or more {@link IObservable}s.
 *
 * <p>Closing the binding detaches every observer it registered. The binding
 * is safe to ignore — observers stay attached for the lifetime of their
 * observables, which is the usual pattern for cross-cutting instrumentation.
 * Closing is idempotent.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ObservabilityBinding implements AutoCloseable {
    private static final IDiagnostic log = Diagnostics.of(ObservabilityBinding.class);

    /**
     * A single (source, wrapper) registration captured at build time so
     * {@link #close()} can detach in the same order.
     */
    public record Registration(IObservable source,
            IObserver<ObservableEvent> wrapper) {
        public Registration {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(wrapper, "wrapper");
        }
    }

    private final List<Registration> registrations;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ObservabilityBinding(List<Registration> registrations) {
        this.registrations = List.copyOf(Objects.requireNonNull(registrations, "registrations"));
    }

    /**
     * @return the number of active (source, wrapper) registrations.
     */
    public int count() {
        return this.registrations.size();
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
}
