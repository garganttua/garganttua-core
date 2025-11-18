package com.garganttua.core.runtime;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.Predefined;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.ContextualObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.Getter;

public class RuntimeContext<InputType, OutputType> extends DiContext implements IRuntimeContext<InputType, OutputType> {

    private InputType input;
    @Getter
    private Class<OutputType> outputType;
    private OutputType output;
    private Map<String, IObjectSupplier<?>> presetVariables = new HashMap<>();
    private Instant start;
    private Instant stop;
    private long startNano;
    private long stopNano;
    private UUID uuid;
    private Integer code;

    public RuntimeContext(IInjectableElementResolver resolver, InputType input, Class<OutputType> outputType,
            Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IDiChildContextFactory<? extends IDiContext>> childContextFactories,
            Map<String, IObjectSupplier<?>> presetVariables) {
        super(resolver, beanProviders, propertyProviders, childContextFactories);
        this.input = Objects.requireNonNull(input, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output type cannot be null");
        this.presetVariables.putAll(Objects.requireNonNull(presetVariables, "Preset variables map cannot be null"));
        this.presetVariables.entrySet().forEach(e -> this.setVariable(e.getKey(), e.getValue().supply().get()));
        this.uuid = UUID.randomUUID();
    }

    @Override
    public IRuntimeResult<InputType, OutputType> getResult() {
        if (this.stop == null)
            throw new RuntimeException("Context is not stopped");

        return new RuntimeResult<>(uuid, input, output, start, stop, startNano, stopNano, code);
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
    public static <ExceptionType extends Throwable, InputType, OutputType> IObjectSupplierBuilder<ExceptionType, IContextualObjectSupplier<ExceptionType, IRuntimeContext<InputType, OutputType>>> exception(
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
        }, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class,
                (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @Override
    public <VariableType> Optional<VariableType> getVariable(String variableName, Class<VariableType> variableType) {
        return this.propertyProviders().get(Predefined.PropertyProviders.garganttua.toString())
                .getProperty(variableName, variableType);
    }

    @Override
    public <ExceptionType> Optional<ExceptionType> getException(Class<ExceptionType> exceptionType) {
        return Optional.empty();
    }

    @Override
    public Optional<InputType> getInput() {
        return Optional.of(this.input);
    }

    @Override
    public Optional<Integer> getCode() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.empty();
    }

    @Override
    public <VariableType> void setVariable(String variableName, VariableType variable) {
        this.propertyProviders().get(Predefined.PropertyProviders.garganttua.toString()).setProperty(variableName,
                variable);
    }

    @Override
    public void setOutput(OutputType output) {
        this.output = Objects.requireNonNull(output, "output cannot be null");
    }

    @Override
    public boolean isOfOutputType(Class<?> type) {
        return this.outputType.isAssignableFrom(type);
    }

    @Override
    public synchronized ILifecycle onInit() throws LifecycleException {
        return super.onInit();
    }

    @Override
    public synchronized ILifecycle onStart() throws LifecycleException {
        this.start = Instant.now();
        this.startNano = System.nanoTime();
        return super.onStart();
    }

    @Override
    public synchronized ILifecycle onFlush() throws LifecycleException {
        super.onFlush();
        this.presetVariables.clear();
        return this;
    }

    @Override
    public synchronized ILifecycle onStop() throws LifecycleException {
        super.onStop();
        this.stop = Instant.now();
        this.stopNano = System.nanoTime();
        return this;
    }

    @Override
    public void setCode(int code) {
        this.code = Objects.requireNonNull(code, "Code cannot be null");
    }

}
