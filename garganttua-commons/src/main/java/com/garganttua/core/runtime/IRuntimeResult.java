package com.garganttua.core.runtime;

import java.time.Duration;

public interface IRuntimeResult<InputType, OutputType> {

    OutputType output();

    Duration getDuration();

    String getPrettyDuration();

    Duration getDurationInMillis();

    long getDurationMillis();

    long getDurationInNanos();

    String getPrettyDurationInNanos();

}
