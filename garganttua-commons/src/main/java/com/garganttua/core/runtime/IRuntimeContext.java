package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.injection.IDiContext;

public interface IRuntimeContext<InputType, OutputType> extends IDiContext {

    IRuntimeResult<InputType, OutputType> getResult();

    <VariableType> Optional<VariableType> getVariable(String variableName, Class<VariableType> variableType);

    Optional<InputType> getInput();

    <ExceptionType> Optional<ExceptionType> getException(Class<ExceptionType> exceptionType);

    Optional<Integer> getCode();

    Optional<String> getExceptionMessage();

}
