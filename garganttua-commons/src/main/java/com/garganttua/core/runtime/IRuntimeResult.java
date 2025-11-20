package com.garganttua.core.runtime;

import java.time.Duration;

public interface IRuntimeResult<InputType, OutputType> {

    OutputType output();

    Duration duration();

    String prettyDuration();

    Duration durationInMillis();

    long durationMillis();

    long durationInNanos();

    String prettyDurationInNanos();

    Integer code();

}
