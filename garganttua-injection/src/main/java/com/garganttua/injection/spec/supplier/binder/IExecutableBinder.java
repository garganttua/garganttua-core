package com.garganttua.injection.spec.supplier.binder;

import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IDiContext;

public interface IExecutableBinder<ExecutionReturn> {

    Optional<ExecutionReturn> execute() throws DiException;

    Optional<ExecutionReturn> execute(IDiContext context)
            throws DiException;

}
