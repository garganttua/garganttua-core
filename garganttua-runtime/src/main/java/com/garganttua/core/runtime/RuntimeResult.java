package com.garganttua.core.runtime;

import com.garganttua.core.observability.Logger;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable outcome of a runtime execution: input/output values, timing, exit
 * code, recorded exceptions, and the final variable snapshot.
 *
 * <p>Also provides helpers to format the execution duration in human-readable
 * form, both colorized (ANSI) and plain.</p>
 *
 * @param uuid               the execution correlation id
 * @param input              the runtime input
 * @param output             the produced output (may be {@code null})
 * @param start              wall-clock start instant
 * @param stop               wall-clock stop instant
 * @param startNano          monotonic start time in nanoseconds
 * @param stopNano           monotonic stop time in nanoseconds
 * @param code               the runtime exit code
 * @param recordedExceptions all exceptions recorded during execution
 * @param variables          final variable values keyed by name
 * @param <InputType>        the input type
 * @param <OutputType>       the output type
 */
public record RuntimeResult<InputType, OutputType>(
        UUID uuid,
        InputType input,
        OutputType output,
        Instant start,
        Instant stop,
        long startNano,
        long stopNano,
        Integer code,
        Set<RuntimeExceptionRecord> recordedExceptions,
        Map<String, Object> variables) implements IRuntimeResult<InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeResult.class);

    @Override
    public boolean hasAborted(){
        log.trace("[RuntimeResult.hasAborted] Checking if runtime has aborted for uuid={}", uuid);
        boolean aborted = this.getAbortingException().isPresent();
        log.debug("[RuntimeResult.hasAborted] Result for uuid={}: {}", uuid, aborted);
        return aborted;
    }

    @Override
    public Optional<RuntimeExceptionRecord> getAbortingException() {
        log.trace("[RuntimeResult.getAbortingException] Searching for aborting exception for uuid={}", uuid);
        Optional<RuntimeExceptionRecord> result = this.recordedExceptions.stream().filter(e -> e.hasAborted()).findFirst();
        log.debug("[RuntimeResult.getAbortingException] Found aborting exception for uuid={}: {}", uuid, result.isPresent());
        return result;
    }

    @Override
    public Set<RuntimeExceptionRecord> getExceptions() {
        log.trace("[RuntimeResult.getExceptions] Retrieving all exceptions for uuid={}", uuid);
        log.debug("[RuntimeResult.getExceptions] Total exceptions for uuid={}: {}", uuid, this.recordedExceptions.size());
        return this.recordedExceptions;
    }

    @Override
    public Duration duration() {
        log.trace("[RuntimeResult.duration] Calculating duration for uuid={}", uuid);
        Duration result = Duration.between(start, stop);
        log.debug("[RuntimeResult.duration] Duration for uuid={}: {}", uuid, result);
        return result;
    }

    @Override
    public Duration durationInMillis() {
        log.trace("[RuntimeResult.durationInMillis] Calculating duration in millis for uuid={}", uuid);
        return Duration.ofMillis(durationMillis());
    }

    @Override
    public long durationMillis() {
        log.trace("[RuntimeResult.durationMillis] Calculating duration millis for uuid={}", uuid);
        long millis = Duration.between(start, stop).toMillis();
        log.debug("[RuntimeResult.durationMillis] Duration millis for uuid={}: {}", uuid, millis);
        return millis;
    }

    @Override
    public long durationInNanos() {
        log.trace("[RuntimeResult.durationInNanos] Calculating duration in nanos for uuid={}", uuid);
        long nanos = stopNano - startNano;
        log.debug("[RuntimeResult.durationInNanos] Duration nanos for uuid={}: {}", uuid, nanos);
        return nanos;
    }

    // =======================================
    // DURATIONS — COLOR
    // =======================================

    @Override
    public String prettyDuration() {
        log.trace("[RuntimeResult.prettyDuration] Formatting pretty duration for uuid={}", uuid);
        return prettyDurationColor(duration());
    }

    /**
     * Formats a duration as an ANSI-colorized {@code h m s ms} string.
     *
     * @param duration the duration to format
     * @return the colorized representation
     */
    public static String prettyDurationColor(Duration duration) {
        log.trace("[RuntimeResult.prettyDurationColor] Formatting duration with color: {}", duration);
        String h = "\u001B[36m";
        String m = "\u001B[35m";
        String s = "\u001B[34m";
        String ms = "\u001B[32m";
        String reset = "\u001B[0m";

        return String.format("%s%dh%s %s%dm%s %s%ds%s %s%dms%s",
                h, duration.toHours(), reset,
                m, duration.toMinutesPart(), reset,
                s, duration.toSecondsPart(), reset,
                ms, duration.toMillisPart(), reset);
    }

    // =======================================
    // DURATIONS — PLAIN (NO COLORS)
    // =======================================

    /**
     * @return this result's duration formatted as a plain {@code h m s ms} string
     */
    public String prettyDurationPlain() {
        log.trace("[RuntimeResult.prettyDurationPlain] Formatting plain pretty duration for uuid={}", uuid);
        return prettyDurationPlain(duration());
    }

    /**
     * Formats a duration as a plain (non-colorized) {@code h m s ms} string.
     *
     * @param duration the duration to format
     * @return the plain representation
     */
    public static String prettyDurationPlain(Duration duration) {
        log.trace("[RuntimeResult.prettyDurationPlain] Formatting duration without color: {}", duration);
        return String.format("%dh %dm %ds %dms",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart(),
                duration.toMillisPart());
    }

    // =======================================
    // NANOS — COLOR
    // =======================================

    /**
     * Formats a nanosecond count as an ANSI-colorized string, scaling to us/ms as needed.
     *
     * @param nanos the nanosecond count
     * @return the colorized representation
     */
    public static String prettyNanoColor(long nanos) {
        log.trace("[RuntimeResult.prettyNanoColor] Formatting nanos with color: {}", nanos);
        String nsColor = "\u001B[36m";
        String usColor = "\u001B[35m";
        String msColor = "\u001B[32m";
        String reset = "\u001B[0m";

        if (nanos < 1_000) {
            return String.format("%s%d ns%s", nsColor, nanos, reset);
        } else if (nanos < 1_000_000) {
            long us = nanos / 1_000;
            return String.format("%s%d ns%s (%s%d us%s)", nsColor, nanos, reset, usColor, us, reset);
        } else {
            long ms = nanos / 1_000_000;
            return String.format("%s%d ns%s (%s%d us%s, %s%d ms%s)",
                    nsColor, nanos, reset,
                    usColor, nanos / 1_000, reset,
                    msColor, ms, reset);
        }
    }

    @Override
    public String prettyDurationInNanos() {
        log.trace("[RuntimeResult.prettyDurationInNanos] Formatting pretty duration in nanos for uuid={}", uuid);
        return prettyNanoColor(durationInNanos());
    }

    // =======================================
    // NANOS — PLAIN (NO COLORS)
    // =======================================

    /**
     * Formats a nanosecond count as a plain string, scaling to us/ms as needed.
     *
     * @param nanos the nanosecond count
     * @return the plain representation
     */
    public static String prettyNano(long nanos) {
        log.trace("[RuntimeResult.prettyNano] Formatting nanos without color: {}", nanos);
        if (nanos < 1_000) {
            return String.format("%d ns", nanos);
        } else if (nanos < 1_000_000) {
            long us = nanos / 1_000;
            return String.format("%d ns (%d us)", nanos, us);
        } else {
            long us = nanos / 1_000;
            long ms = nanos / 1_000_000;
            return String.format("%d ns (%d us, %d ms)", nanos, us, ms);
        }
    }

    /**
     * @return this result's nanosecond duration formatted as a plain string
     */
    public String prettyDurationInNanosPlain() {
        log.trace("[RuntimeResult.prettyDurationInNanosPlain] Formatting plain pretty duration in nanos for uuid={}", uuid);
        return prettyNano(durationInNanos());
    }
}