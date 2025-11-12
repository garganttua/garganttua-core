package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeOperationStepBuilder<ExecutionReturn> extends
        AbstractMethodBinderBuilder<ExecutionReturn, IRuntimeStepOperationBuilder<ExecutionReturn>, IRuntimeStepBuilder>
        implements IRuntimeStepOperationBuilder<ExecutionReturn> {

    private String storeReturnInVariable = null;
    private Boolean isOutput;

    protected RuntimeOperationStepBuilder(IRuntimeStepBuilder up, IObjectSupplierBuilder<?, ?> supplier,
            boolean collection) throws DslException {
        super(up, supplier, collection);
    }

    protected RuntimeOperationStepBuilder(IRuntimeStepBuilder up, IObjectSupplierBuilder<?, ?> supplier)
            throws DslException {
        super(up, supplier);
    }

    @Override
    protected IRuntimeStepOperationBuilder<ExecutionReturn> getBuilder() {
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        
    }

    @Override
    public IRuntimeStepOperationBuilder<ExecutionReturn> variable(String variableName) {
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepOperationBuilder<ExecutionReturn> output(boolean output) {
        this.isOutput = Objects.requireNonNull(output, "Output cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepCatchBuilder katch(Class<? extends Throwable> exception) throws DslException {
        return new RuntimeStepCatchBuilder(exception, this.findMethod(), this);
    }

}
