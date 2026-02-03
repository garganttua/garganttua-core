package com.garganttua.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    @Override
    public boolean hasAborted(){
        log.atTrace().log("[RuntimeResult.hasAborted] Checking if runtime has aborted for uuid={}", uuid);
        boolean aborted = this.getAbortingException().isPresent();
        log.atDebug().log("[RuntimeResult.hasAborted] Result for uuid={}: {}", uuid, aborted);
        return aborted;
    }

    @Override
    public Optional<RuntimeExceptionRecord> getAbortingException() {
        log.atTrace().log("[RuntimeResult.getAbortingException] Searching for aborting exception for uuid={}", uuid);
        Optional<RuntimeExceptionRecord> result = this.recordedExceptions.stream().filter(e -> e.hasAborted()).findFirst();
        log.atDebug().log("[RuntimeResult.getAbortingException] Found aborting exception for uuid={}: {}", uuid, result.isPresent());
        return result;
    }

    @Override
    public Set<RuntimeExceptionRecord> getExceptions() {
        log.atTrace().log("[RuntimeResult.getExceptions] Retrieving all exceptions for uuid={}", uuid);
        log.atDebug().log("[RuntimeResult.getExceptions] Total exceptions for uuid={}: {}", uuid, this.recordedExceptions.size());
        return this.recordedExceptions;
    }

    @Override
    public Duration duration() {
        log.atTrace().log("[RuntimeResult.duration] Calculating duration for uuid={}", uuid);
        Duration result = Duration.between(start, stop);
        log.atDebug().log("[RuntimeResult.duration] Duration for uuid={}: {}", uuid, result);
        return result;
    }

    @Override
    public Duration durationInMillis() {
        log.atTrace().log("[RuntimeResult.durationInMillis] Calculating duration in millis for uuid={}", uuid);
        return Duration.ofMillis(durationMillis());
    }

    @Override
    public long durationMillis() {
        log.atTrace().log("[RuntimeResult.durationMillis] Calculating duration millis for uuid={}", uuid);
        long millis = Duration.between(start, stop).toMillis();
        log.atDebug().log("[RuntimeResult.durationMillis] Duration millis for uuid={}: {}", uuid, millis);
        return millis;
    }

    @Override
    public long durationInNanos() {
        log.atTrace().log("[RuntimeResult.durationInNanos] Calculating duration in nanos for uuid={}", uuid);
        long nanos = stopNano - startNano;
        log.atDebug().log("[RuntimeResult.durationInNanos] Duration nanos for uuid={}: {}", uuid, nanos);
        return nanos;
    }

    // =======================================
    // DURATIONS — COLOR
    // =======================================

    @Override
    public String prettyDuration() {
        log.atTrace().log("[RuntimeResult.prettyDuration] Formatting pretty duration for uuid={}", uuid);
        return prettyDurationColor(duration());
    }

    public static String prettyDurationColor(Duration duration) {
        log.atTrace().log("[RuntimeResult.prettyDurationColor] Formatting duration with color: {}", duration);
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

    public String prettyDurationPlain() {
        log.atTrace().log("[RuntimeResult.prettyDurationPlain] Formatting plain pretty duration for uuid={}", uuid);
        return prettyDurationPlain(duration());
    }

    public static String prettyDurationPlain(Duration duration) {
        log.atTrace().log("[RuntimeResult.prettyDurationPlain] Formatting duration without color: {}", duration);
        return String.format("%dh %dm %ds %dms",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart(),
                duration.toMillisPart());
    }

    // =======================================
    // NANOS — COLOR
    // =======================================

    public static String prettyNanoColor(long nanos) {
        log.atTrace().log("[RuntimeResult.prettyNanoColor] Formatting nanos with color: {}", nanos);
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
        log.atTrace().log("[RuntimeResult.prettyDurationInNanos] Formatting pretty duration in nanos for uuid={}", uuid);
        return prettyNanoColor(durationInNanos());
    }

    // =======================================
    // NANOS — PLAIN (NO COLORS)
    // =======================================

    public static String prettyNano(long nanos) {
        log.atTrace().log("[RuntimeResult.prettyNano] Formatting nanos without color: {}", nanos);
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
        log.atTrace().log("[RuntimeResult.prettyDurationInNanosPlain] Formatting plain pretty duration in nanos for uuid={}", uuid);
        return prettyNano(durationInNanos());
    }
}