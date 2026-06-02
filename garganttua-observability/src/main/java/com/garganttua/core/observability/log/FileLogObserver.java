package com.garganttua.core.observability.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableEvent;

/**
 * {@link IObserver} that appends formatted events to a file, one line per
 * event. Pure {@code java.nio.file} — no external dependency.
 *
 * <p>Writes are synchronized on the observer instance to keep concurrent
 * producers from interleaving partial lines. The default formatter is
 * {@link JsonLineEventFormatter} (NDJSON, directly ingestible by Elastic-
 * search / Loki / Filebeat); switch to {@link PlainTextEventFormatter} for
 * tail-friendly output.
 *
 * <p>Rotation is intentionally not handled here — production deployments
 * route this through {@code logrotate} or equivalent. A future
 * {@code RotatingFileLogObserver} can layer on top without changing this
 * class.
 *
 * <p>Implements {@link AutoCloseable}: {@code close()} flushes and releases
 * the underlying file handle. Closing is idempotent. After close, further
 * {@link #onEvent(ObservableEvent)} calls are silent no-ops (events are
 * dropped rather than throwing — a logging observer must never poison its
 * producer).
 *
 * <p>Build via the fluent {@link Builder}:
 * <pre>{@code
 * try (FileLogObserver obs = FileLogObserver.builder()
 *         .path(Path.of("/var/log/garganttua/events.ndjson"))
 *         .formatter(JsonLineEventFormatter.INSTANCE)
 *         .append(true)
 *         .build()) {
 *     // … attach via ObservabilityBuilder.observer(obs) …
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public final class FileLogObserver implements IObserver<ObservableEvent>, AutoCloseable {

    private final Path path;
    private final IEventFormatter formatter;
    private final BufferedWriter writer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private FileLogObserver(Path path, IEventFormatter formatter, boolean append) {
        this.path = Objects.requireNonNull(path, "path");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            OpenOption[] opts = append
                    ? new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND }
                    : new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };
            Writer base = Files.newBufferedWriter(path, StandardCharsets.UTF_8, opts);
            // Wrap once more so flush() costs less than the JDK's default
            // ChannelWriter flush. Synchronized write/flush at every event
            // would erase the benefit of buffering — instead we flush after
            // each event so a crash never loses more than the current write.
            this.writer = (base instanceof BufferedWriter bw) ? bw : new BufferedWriter(base);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open log file: " + path, e);
        }
    }

    /**
     * Format {@code event}, append it as one line to the file, and flush.
     * Null events and events received after {@link #close()} are silently
     * dropped; the write is synchronized so concurrent producers cannot
     * interleave partial lines.
     *
     * @param event the event to render, may be {@code null}
     * @throws UncheckedIOException if writing to the file fails
     */
    @Override
    public void onEvent(ObservableEvent event) {
        if (event == null || this.closed.get()) {
            return;
        }
        String line = this.formatter.format(event);
        synchronized (this) {
            if (this.closed.get()) {
                return;
            }
            try {
                this.writer.write(line);
                this.writer.newLine();
                this.writer.flush();
            } catch (IOException e) {
                // Swallow: a misbehaving log sink must NEVER throw into the
                // producer. The ObservableRegistry catches RuntimeException
                // already, but propagating IOException as runtime would still
                // pollute the producer's stack trace.
                throw new UncheckedIOException("Failed to write event to " + this.path, e);
            }
        }
    }

    /**
     * Flush and release the underlying file handle. Idempotent; subsequent
     * {@link #onEvent(ObservableEvent)} calls become silent no-ops. Flush and
     * close failures are swallowed (best-effort) so closing never throws.
     */
    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (this) {
            try {
                this.writer.flush();
            } catch (IOException ignored) {
                // best-effort flush
            }
            try {
                this.writer.close();
            } catch (IOException ignored) {
                // best-effort close
            }
        }
    }

    /**
     * @return the file path this observer writes to.
     */
    public Path path() {
        return this.path;
    }

    /**
     * @return whether {@link #close()} has been invoked successfully.
     */
    public boolean isClosed() {
        return this.closed.get();
    }

    /**
     * Start building a {@link FileLogObserver}.
     *
     * @return a fresh {@link Builder} defaulting to
     *         {@link JsonLineEventFormatter} and append mode
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link FileLogObserver}.
     */
    public static final class Builder {
        private Path path;
        private IEventFormatter formatter = JsonLineEventFormatter.INSTANCE;
        private boolean append = true;

        private Builder() {
        }

        /**
         * Target file path (required). Parent directories are created on build.
         */
        public Builder path(Path path) {
            this.path = Objects.requireNonNull(path, "path");
            return this;
        }

        /**
         * Formatter used to render each event. Defaults to
         * {@link JsonLineEventFormatter#INSTANCE}.
         */
        public Builder formatter(IEventFormatter formatter) {
            this.formatter = Objects.requireNonNull(formatter, "formatter");
            return this;
        }

        /**
         * If {@code true} (default), open the file in append mode so a restart
         * preserves prior log content. If {@code false}, truncate on open —
         * primarily useful for tests.
         */
        public Builder append(boolean append) {
            this.append = append;
            return this;
        }

        /**
         * Open the target file and build the {@link FileLogObserver}. Parent
         * directories are created if missing.
         *
         * @return a new observer holding an open file handle
         * @throws IllegalStateException if no path was set via {@link #path(Path)}
         * @throws UncheckedIOException  if the file cannot be opened
         */
        public FileLogObserver build() {
            if (this.path == null) {
                throw new IllegalStateException("path must be set via .path(...) before build()");
            }
            return new FileLogObserver(this.path, this.formatter, this.append);
        }
    }
}
