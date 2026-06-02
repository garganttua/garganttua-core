package com.garganttua.core.observability.dsl;

import java.util.Objects;
import java.util.function.Predicate;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;

/**
 * Wraps a target {@link IObserver} with a per-subscription filter. Two filter
 * shapes are supported:
 * <ul>
 *   <li>A pre-built {@link ICondition} that reads the current event from an
 *       {@link EventHolderSupplier} the framework refreshes before each call.</li>
 *   <li>A JDK {@link Predicate} that tests the event directly.</li>
 * </ul>
 * If both are present they compose with AND. If neither is present the wrapper
 * forwards every event.
 *
 * <p>Package-private: only the builder constructs these.
 *
 * @since 2.0.0-ALPHA02
 */
final class FilteredObserver implements IObserver<ObservableEvent> {
    private static final Logger log = Logger.getLogger(FilteredObserver.class);

    private final IObserver<ObservableEvent> target;
    private final ICondition condition;
    private final EventHolderSupplier holder;
    private final Predicate<ObservableEvent> predicate;

    FilteredObserver(IObserver<ObservableEvent> target,
            ICondition condition, EventHolderSupplier holder,
            Predicate<ObservableEvent> predicate) {
        this.target = Objects.requireNonNull(target, "target observer");
        this.condition = condition;
        this.holder = holder;
        this.predicate = predicate;
    }

    @Override
    public void onEvent(ObservableEvent event) {
        if (event == null) {
            return;
        }
        if (this.predicate != null) {
            try {
                if (!this.predicate.test(event)) {
                    return;
                }
            } catch (RuntimeException e) {
                log.warn("Predicate filter threw, dropping event {}: {}",
                        event.getClass().getSimpleName(), e.getMessage());
                return;
            }
        }
        if (this.condition != null) {
            this.holder.setCurrent(event);
            try {
                Boolean match = this.condition.fullEvaluate();
                if (match == null || !match) {
                    return;
                }
            } catch (ExpressionException e) {
                log.warn("Condition filter threw, dropping event {}: {}",
                        event.getClass().getSimpleName(), e.getMessage());
                return;
            }
        }
        this.target.onEvent(event);
    }
}
