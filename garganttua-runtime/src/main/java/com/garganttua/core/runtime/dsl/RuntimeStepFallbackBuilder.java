package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.context.dsl.AbstractMethodArgInjectBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> extends
        AbstractMethodArgInjectBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType>>
        implements
        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> {

    private String storeReturnInVariable = null;
    private Boolean output = false;

    protected RuntimeStepFallbackBuilder(IRuntimeStepBuilder<ExecutionReturn, StepObjectType> up,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier,
            IInjectableElementResolver resolver)
            throws DslException {
        super(resolver, up, supplier);
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> variable(String variableName) {
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> output(boolean output) {
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        return this;
    }

    @Override
    public void handle(IDiContext context) {
        Objects.requireNonNull(context, "Context cannot be null");
        this.setResolver(context);
    }

}
