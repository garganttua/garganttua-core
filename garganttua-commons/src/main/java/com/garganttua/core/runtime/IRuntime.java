package com.garganttua.core.runtime;

import java.util.Optional;
import java.util.UUID;

public interface IRuntime<InputType, OutputType> {

    public static final int GENERIC_RUNTIME_SUCCESS_CODE = 0;
    public static final int GENERIC_RUNTIME_ERROR_CODE = 50;

    Optional<IRuntimeResult<InputType, OutputType>> execute(InputType input) throws RuntimeException;

    Optional<IRuntimeResult<InputType, OutputType>> execute(UUID uuid, InputType input) throws RuntimeException;

}
