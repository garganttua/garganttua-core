package com.garganttua.core.observability.log;

import java.io.PrintStream;
import java.util.Objects;

import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;

/**
 * {@link IObserver} that prints formatted events to a {@link PrintStream},
 * defaulting to {@link System#out}.
 *
 * <p>Writes are synchronized on the observer instance so multi-thread
 * producers cannot interleave bytes from concurrent events. The default
 * formatter is {@link PlainTextEventFormatter} for human readability;
 * production setups will typically wire {@link JsonLineEventFormatter}
 * with stdout redirected to a log shipper (Filebeat, Vector, …).
 *
 * <p>Build via the fluent {@link Builder}:
 * <pre>{@code
 * IObserver<ObservableEvent> observer = ConsoleLogObserver.builder()
 *         .formatter(JsonLineEventFormatter.INSTANCE)
 *         .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public final class ConsoleLogObserver implements IObserver<ObservableEvent> {

    private final PrintStream stream;
    private final IEventFormatter formatter;

    private ConsoleLogObserver(PrintStream stream, IEventFormatter formatter) {
        this.stream = Objects.requireNonNull(stream, "stream");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    /**
     * Format {@code event} and print it as one line to the configured stream.
     * Null events are ignored; the write is synchronized so concurrent
     * producers cannot interleave bytes.
     *
     * @param event the event to render, may be {@code null}
     */
    @Override
    public void onEvent(ObservableEvent event) {
        if (event == null) {
            return;
        }
        String line = this.formatter.format(event);
        synchronized (this) {
            this.stream.println(line);
        }
    }

    /**
     * Start building a {@link ConsoleLogObserver}.
     *
     * @return a fresh {@link Builder} defaulting to {@link System#out} and
     *         {@link PlainTextEventFormatter}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ConsoleLogObserver}.
     */
    public static final class Builder {
        private PrintStream stream = System.out;
        private IEventFormatter formatter = PlainTextEventFormatter.INSTANCE;

        private Builder() {
        }

        /**
         * Stream to write to. Defaults to {@link System#out}.
         */
        public Builder stream(PrintStream stream) {
            this.stream = Objects.requireNonNull(stream, "stream");
            return this;
        }

        /**
         * Formatter used to render each event. Defaults to
         * {@link PlainTextEventFormatter#INSTANCE}.
         */
        public Builder formatter(IEventFormatter formatter) {
            this.formatter = Objects.requireNonNull(formatter, "formatter");
            return this;
        }

        /**
         * Build the configured {@link ConsoleLogObserver}.
         *
         * @return a new immutable observer
         */
        public ConsoleLogObserver build() {
            return new ConsoleLogObserver(this.stream, this.formatter);
        }
    }
}
