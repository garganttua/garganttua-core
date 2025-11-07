package com.garganttua.core.runtime;

public interface IRuntime<InputType, OutputType> {

    IRuntimeResult<InputType, OutputType> execute(InputType input) throws RuntimeException;

}
