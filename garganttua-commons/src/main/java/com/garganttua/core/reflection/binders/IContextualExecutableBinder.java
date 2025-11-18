package com.garganttua.core.reflection.binders;

import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;

public interface IContextualExecutableBinder<ExecutionReturn, OwnerContextType>
                extends IExecutableBinder<ExecutionReturn> {

        Class<OwnerContextType> getOwnerContextType();

        Class<?>[] getParametersContextTypes();

        Optional<ExecutionReturn> execute(OwnerContextType ownerContext, Object... contexts)
                        throws ReflectionException;

        @Override
        default Optional<ExecutionReturn> execute() throws ReflectionException {
                throw new ReflectionException(
                                "Owner context of type " + getOwnerContextType().getSimpleName()
                                                + " required for this supplier");
        }

}
