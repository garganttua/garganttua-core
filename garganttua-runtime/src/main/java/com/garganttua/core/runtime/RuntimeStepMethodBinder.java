package com.garganttua.core.runtime;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public class RuntimeStepMethodBinder<ExecutionReturned, Input, Output> implements IRuntimeStepMethodBinder<ExecutionReturned, IRuntimeContext<Input, Output>> {

    public static final int GENERIC_RUNTIME_SUCCESS_CODE = 0;

    public static final Integer GENERIC_RUNTIME_ERROR_CODE = null;

    private IContextualMethodBinder<ExecutionReturned, IRuntimeContext<Input, Output>> delegate;
    private Optional<String> variable;
    private boolean isOutput;

    private Integer successCode;

    public RuntimeStepMethodBinder(IContextualMethodBinder<ExecutionReturned, IRuntimeContext<Input, Output>> delegate,
            Optional<String> variable, boolean isOutput, Integer successCode) {
                this.delegate = delegate;
                this.variable = variable;
                this.isOutput = isOutput;
                this.successCode = successCode;
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return this.delegate.getDependencies();
    }

    @Override
    public Class<IRuntimeContext<Input, Output>> getOwnerContextType() {
        return this.delegate.getOwnerContextType();
    }

    @Override
    public Class<?>[] getParametersContextTypes() {
        return this.delegate.getParametersContextTypes();
    }

    @Override
    public Optional<ExecutionReturned> execute(IRuntimeContext<Input, Output> ownerContext, Object... contexts) throws ReflectionException {
        return this.delegate.execute(ownerContext, contexts);
    }

    @Override
    public boolean isOutput() {
        return this.isOutput;
    }

    @Override
    public void setSuccessCode(IRuntimeContext<?, ?> c) {
        c.setCode(successCode);
    }

    @Override
    public String getExecutableReference() {
        return this.delegate.getExecutableReference();
    }
}
