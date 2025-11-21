package com.garganttua.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RuntimeResult<InputType, OutputType>(
        UUID uuid,
        InputType input,
        OutputType output,
        Instant start,
        Instant stop,
        long startNano,
        long stopNano,
        Integer code,
        Set<RuntimeExceptionRecord> recordedException
) implements IRuntimeResult<InputType, OutputType> {

    @Override
    public Duration duration() {
        return Duration.between(start, stop);
    }

    @Override
    public Duration durationInMillis() {
        return Duration.ofMillis(durationMillis());
    }

    @Override
    public long durationMillis() {
        return Duration.between(start, stop).toMillis();
    }

    @Override
    public long durationInNanos() {
        return stopNano - startNano;
    }

    // =======================================
    //  DURATIONS — COLOR
    // =======================================

    @Override
    public String prettyDuration() {
        return prettyDurationColor(duration());
    }

    public static String prettyDurationColor(Duration duration) {
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
    //  DURATIONS — PLAIN (NO COLORS)
    // =======================================

    public String prettyDurationPlain() {
        return prettyDurationPlain(duration());
    }

    public static String prettyDurationPlain(Duration duration) {
        return String.format("%dh %dm %ds %dms",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart(),
                duration.toMillisPart());
    }

    // =======================================
    //  NANOS — COLOR
    // =======================================

    public static String prettyNanoColor(long nanos) {
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
        return prettyNanoColor(durationInNanos());
    }

    // =======================================
    //  NANOS — PLAIN (NO COLORS)
    // =======================================

    public static String prettyNano(long nanos) {
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

    public String prettyDurationInNanosPlain() {
        return prettyNano(durationInNanos());
    }
}