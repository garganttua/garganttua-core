package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.injection.IDiContext;

public interface IRuntimeContext<InputType, OutputType> extends IDiContext {

    IRuntimeResult<InputType, OutputType> getResult();

    <VariableType> void setVariable(String variableName, VariableType variable);

    <VariableType> Optional<VariableType> getVariable(String variableName, Class<VariableType> variableType);

    Optional<InputType> getInput();

    <ExceptionType> Optional<ExceptionType> getException(Class<ExceptionType> exceptionType);

    Optional<Integer> getCode();

    Optional<String> getExceptionMessage();

    void setOutput(OutputType output);

    boolean isOfOutputType(Class<?> class1);

    Class<?> getOutputType();

    void setCode(int i);

    void recordException(RuntimeExceptionRecord runtimeExceptionRecord);

    RuntimeExceptionRecord findException(RuntimeExceptionRecord pattern);

}
