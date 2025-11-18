package com.garganttua.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record RuntimeResult<InputType, OutputType>(UUID uuid, InputType input, OutputType output, Instant start,
        Instant stop, Integer code) implements IRuntimeResult<InputType, OutputType> {

    @Override
    public Duration getDuration() {
        return Duration.between(start, stop);
    }

    @Override
    public String getPrettyDuration() {
        Duration duration = getDuration();
        return String.format(
                "\u001B[36m%dh\u001B[0m "
                        + "\u001B[32m%dm\u001B[0m "
                        + "\u001B[33m%ds\u001B[0m "
                        + "\u001B[35m%dms\u001B[0m",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart(),
                duration.toMillisPart());
    }

}
