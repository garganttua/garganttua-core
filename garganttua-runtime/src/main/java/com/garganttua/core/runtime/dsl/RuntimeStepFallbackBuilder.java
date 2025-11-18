package com.garganttua.core.runtime.dsl;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.context.dsl.AbstractMethodArgInjectBinderBuilder;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepMethodBinder;
import com.garganttua.core.runtime.RuntimeStepMethodBinder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractMethodArgInjectBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>>
        implements
        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private String storeReturnInVariable = null;
    private Boolean output = false;
    private Integer code = RuntimeStepMethodBinder.GENERIC_RUNTIME_ERROR_CODE;

    protected RuntimeStepFallbackBuilder(IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> up,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier,
            IInjectableElementResolver resolver)
            throws DslException {
        super(Optional.ofNullable(resolver), up, supplier);
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(String variableName) {
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output) {
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        return this;
    }

    @Override
    public void handle(IDiContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        this.setResolver(context);
    }

    @Override
    public IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> build() throws DslException {
        IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> binder = (IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>) super.build();
        return new RuntimeStepMethodBinder<ExecutionReturn, InputType, OutputType>(binder, Optional.ofNullable(this.storeReturnInVariable), this.output, this.code);
    }

}
