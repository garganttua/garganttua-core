package com.garganttua.core.reflection.binders;

import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;

public interface IExecutableBinder<ExecutionReturn> extends Dependent {

    String getExecutableReference();

    Optional<ExecutionReturn> execute() throws ReflectionException;

}
