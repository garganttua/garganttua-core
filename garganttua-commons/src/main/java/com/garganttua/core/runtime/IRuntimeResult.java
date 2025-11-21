package com.garganttua.core.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface IRuntimeResult<InputType, OutputType> {

    OutputType output();

    Duration duration();

    String prettyDuration();

    Duration durationInMillis();

    long durationMillis();

    long durationInNanos();

    String prettyDurationInNanos();

    Integer code();

    Set<RuntimeExceptionRecord> getExceptions();

    Optional<RuntimeExceptionRecord> getAbortingException();

    boolean hasAborted();

}
