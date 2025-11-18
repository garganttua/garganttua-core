package com.garganttua.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record RuntimeResult<InputType, OutputType>(UUID uuid, InputType input, OutputType output, Instant start,
        Instant stop,
        long startNano,
        long stopNano,
        Integer code) implements IRuntimeResult<InputType, OutputType> {

    @Override
    public Duration getDuration() {
        return Duration.between(start, stop);
    }

    @Override
    public Duration getDurationInMillis() {
        return Duration.ofMillis(getDurationMillis());
    }

    @Override
    public long getDurationMillis() {
        return Duration.between(start, stop).toMillis();
    }

    @Override
    public long getDurationInNanos() {
        return stopNano - startNano;
    }

    // --- Pretty print ---
    @Override
    public String getPrettyDuration() {
        return prettyDurationColor(getDuration());
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

    public static String prettyNano(long nanos) {
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
    public String getPrettyDurationInNanos() {
        return prettyNano(getDurationInNanos());
    }

}
