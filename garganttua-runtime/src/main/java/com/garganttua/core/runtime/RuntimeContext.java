package com.garganttua.core.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.ContextualObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeContext<InputType, OutputType> extends DiContext implements IRuntimeContext<InputType, OutputType> {

    private InputType input;
    private Class<OutputType> outputType;

    public RuntimeContext(InputType input, Class<OutputType> outputType, Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IDiChildContextFactory<? extends IDiContext>> childContextFactories) {
        super(beanProviders, propertyProviders, childContextFactories);
        this.input = input;
        this.outputType = outputType;
        this.initialized.set(true);
        this.started.set(true);
    }

    @Override
    public IRuntimeResult<InputType, OutputType> getResult() {
        return null;
    }

    // Static Supplier Builders

    @SuppressWarnings("unchecked")
    public static <VariableType, InputType, OutputType> IObjectSupplierBuilder<VariableType, IContextualObjectSupplier<VariableType, IRuntimeContext<InputType, OutputType>>> variable(
            String variableName, Class<VariableType> variableType) {

        return new ContextualObjectSupplierBuilder<>((context, others) -> {
            return context.getVariable(variableName, variableType);
        }, variableType, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);

    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> IObjectSupplierBuilder<InputType, IContextualObjectSupplier<InputType, IRuntimeContext<InputType, OutputType>>> input(Class<InputType> inputType) {
        return new ContextualObjectSupplierBuilder<>((context, others) -> {
            return context.getInput();
        }, inputType, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    public static <InputType, OutputType> IObjectSupplierBuilder<InputType, IContextualObjectSupplier<InputType, IRuntimeContext<InputType, OutputType>>> exception() {
        return null;
    }

    public static <InputType, OutputType> IObjectSupplierBuilder<InputType, IContextualObjectSupplier<InputType, IRuntimeContext<InputType, OutputType>>> code() {
        return null;
    }

    public static <InputType, OutputType> IObjectSupplierBuilder<InputType, IContextualObjectSupplier<InputType, IRuntimeContext<InputType, OutputType>>> exceptionMessage() {
        return null;
    }

    public static <InputType, OutputType> IObjectSupplierBuilder<InputType, IContextualObjectSupplier<InputType, IRuntimeContext<InputType, OutputType>>> context() {
        return null;
    }

    @Override
    public <VariableType> Optional<VariableType> getVariable(String variableName, Class<VariableType> variableType) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getVariable'");
    }

    @Override
    public Optional<InputType> getInput() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInput'");
    }

}
