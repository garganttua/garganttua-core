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
    public static <InputType, OutputType> IObjectSupplierBuilder<InputType, IContextualObjectSupplier<InputType, IRuntimeContext<InputType, OutputType>>> input(
            Class<InputType> inputType) {
        return new ContextualObjectSupplierBuilder<>((context, others) -> {
            return context.getInput();
        }, inputType, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <ExceptionType, InputType, OutputType> IObjectSupplierBuilder<ExceptionType, IContextualObjectSupplier<ExceptionType, IRuntimeContext<InputType, OutputType>>> exception(
            Class<ExceptionType> exceptionType) {
        return new ContextualObjectSupplierBuilder<>((context, others) -> {
            return context.getException(exceptionType);
        }, exceptionType, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> IObjectSupplierBuilder<Integer, IContextualObjectSupplier<Integer, IRuntimeContext<InputType, OutputType>>> code() {
        return new ContextualObjectSupplierBuilder<>((context, others) -> {
            return context.getCode();
        }, Integer.class, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> IObjectSupplierBuilder<String, IContextualObjectSupplier<String, IRuntimeContext<InputType, OutputType>>> exceptionMessage() {
        return new ContextualObjectSupplierBuilder<>((context, others) -> {
            return context.getExceptionMessage();
        }, String.class, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> IObjectSupplierBuilder<IRuntimeContext<InputType, OutputType>, IContextualObjectSupplier<IRuntimeContext<InputType, OutputType>, IRuntimeContext<InputType, OutputType>>> context() {
        return new ContextualObjectSupplierBuilder<>((context, others) -> {
            return Optional.of(context);
        }, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @Override
    public <VariableType> Optional<VariableType> getVariable(String variableName, Class<VariableType> variableType) {
        return Optional.empty();
    }

    @Override
    public <ExceptionType> Optional<ExceptionType> getException(Class<ExceptionType> exceptionType) {
        return Optional.empty();
    }

    @Override
    public Optional<InputType> getInput() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getCode() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.empty();
    }

}
