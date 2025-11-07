package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeOperationStepBuilder<ExecutionReturn> extends
        AbstractMethodBinderBuilder<ExecutionReturn, IRuntimeOperationStepBuilder<ExecutionReturn>, IRuntimeStepBuilder>
        implements IRuntimeOperationStepBuilder<ExecutionReturn> {

    private String storeReturnInVariable = null;

    protected RuntimeOperationStepBuilder(IRuntimeStepBuilder up, IObjectSupplierBuilder<?, ?> supplier,
            boolean collection) throws DslException {
        super(up, supplier, collection);
    }

    protected RuntimeOperationStepBuilder(IRuntimeStepBuilder up, IObjectSupplierBuilder<?, ?> supplier)
            throws DslException {
        super(up, supplier);
    }

    @Override
    public IRuntimeOperationStepBuilder<ExecutionReturn> storeReturn(String variableName) {
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        return this;
    }

    @Override
    protected IRuntimeOperationStepBuilder<ExecutionReturn> getBuilder() {
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        
    }

}
