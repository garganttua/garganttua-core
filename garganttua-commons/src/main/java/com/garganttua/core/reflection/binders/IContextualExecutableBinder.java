package com.garganttua.core.reflection.binders;

import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;

public interface IContextualExecutableBinder<ExecutionReturn, OwnerContextType> extends Dependent {

    Class<OwnerContextType> getOwnerContextType();

    Class<?>[] getParametersContextTypes();

    Optional<ExecutionReturn> execute(OwnerContextType ownerContext, Object... contexts)
            throws ReflectionException;

}
