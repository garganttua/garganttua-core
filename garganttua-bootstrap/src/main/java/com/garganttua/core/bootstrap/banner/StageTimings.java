package com.garganttua.core.bootstrap.banner;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-stage timing accumulator collected during a single Bootstrap build.
 *
 * <p>Stages keyed by their canonical name (e.g. {@code "resolve"},
 * {@code "configure"}, {@code "build"}). Multiple invocations of the same
 * stage name accumulate — useful for the per-builder build sub-phase which
 * fires once per registered builder.
 *
 * <p>Backed by a {@link LinkedHashMap} so the iteration order matches the
 * insertion order — the banner reads it sequentially.
 *
 * @since 2.0.0-ALPHA02
 */
public final class StageTimings {

    private final Map<String, Duration> totals = new LinkedHashMap<>();

    /**
     * Record an elapsed duration for the given stage. Subsequent calls with
     * the same stage name add to the accumulator. Null arguments are ignored.
     *
     * @param stage   the stage name key
     * @param elapsed the elapsed duration to add
     */
    public void record(String stage, Duration elapsed) {
        if (stage == null || elapsed == null) {
            return;
        }
        this.totals.merge(stage, elapsed, Duration::plus);
    }

    /**
     * Time a runnable section and record the elapsed under the given stage.
     * The duration is recorded even if {@code body} throws.
     *
     * @param stage the stage name key
     * @param body  the section to time
     */
    public void time(String stage, Runnable body) {
        Instant start = Instant.now();
        try {
            body.run();
        } finally {
            record(stage, Duration.between(start, Instant.now()));
        }
    }

    /**
     * @return an immutable snapshot of every accumulated stage.
     */
    public Map<String, Duration> snapshot() {
        return Map.copyOf(this.totals);
    }

    /**
     * Format a duration human-readably for the banner. Sub-millisecond
     * intervals fall back to microseconds; otherwise ms is the unit.
     */
    public static String format(Duration d) {
        long us = d.toNanos() / 1_000;
        if (us < 1_000) {
            return us + "µs";
        }
        return d.toMillis() + "ms";
    }
}
