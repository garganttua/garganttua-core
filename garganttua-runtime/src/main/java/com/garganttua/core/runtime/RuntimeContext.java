package com.garganttua.core.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.Predefined;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.ContextualObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.CopyException;

import lombok.Getter;

public class RuntimeContext<InputType, OutputType> extends AbstractLifecycle
        implements IRuntimeContext<InputType, OutputType> {

    private final InputType input;
    @Getter
    private final Class<OutputType> outputType;
    private OutputType output;
    private final Map<String, IObjectSupplier<?>> presetVariables = new HashMap<>();
    private Instant start;
    private Instant stop;
    private long startNano;
    private long stopNano;
    private final UUID uuid;
    private Integer code;
    private final IDiContext delegateContext;

    private final Object lifecycleMutex = new Object();
    private final Set<RuntimeExceptionRecord> recordedException = new HashSet<>();

    public RuntimeContext(IDiContext parent, InputType input, Class<OutputType> outputType,
            Map<String, IObjectSupplier<?>> presetVariables) {
        this.delegateContext = Objects.requireNonNull(parent, "Parent context cannot be null");
        this.input = Objects.requireNonNull(input, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output type cannot be null");
        this.presetVariables
                .putAll(Map.copyOf(Objects.requireNonNull(presetVariables, "Preset variables map cannot be null")));
        this.uuid = UUID.randomUUID();
    }

    @Override
    public IRuntimeResult<InputType, OutputType> getResult() {
        wrapLifecycle(this::ensureStopped, RuntimeException.class);
        wrapLifecycle(this::ensureNotFlushed, RuntimeException.class);

        return new RuntimeResult<>(uuid, input, output, start, stop, startNano, stopNano, code, this.recordedException);
    }

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
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        return this.delegateContext
                .getProperty(Predefined.PropertyProviders.garganttua.toString(), variableName, variableType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ExceptionType> Optional<ExceptionType> getException(Class<ExceptionType> exceptionType) {
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        Optional<RuntimeExceptionRecord> report = this.findAbortingExceptionReport();
        if (report.isPresent()) {
            if( exceptionType.isAssignableFrom(report.get().exceptionType()) ){
                return (Optional<ExceptionType>) Optional.of(report.get().exception());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<InputType> getInput() {
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        return Optional.of(this.input);
    }

    @Override
    public Optional<Integer> getCode() {
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        return Optional.of(this.code);
    }

    @Override
    public Optional<String> getExceptionMessage() {
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);

        String message = null;
        Optional<RuntimeExceptionRecord> report = this.findAbortingExceptionReport();
        if (report.isPresent()) {
            message = report.get().exceptionMessage();
        }

        return Optional.ofNullable(message);
    }

    @Override
    public <VariableType> void setVariable(String variableName, VariableType variable) {
        wrapLifecycle(this::ensureInitialized, RuntimeException.class);
        this.delegateContext.setProperty(Predefined.PropertyProviders.garganttua.toString(), variableName,
                variable);
    }

    @Override
    public void setOutput(OutputType output) {
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        this.output = Objects.requireNonNull(output, "output cannot be null");
    }

    @Override
    public boolean isOfOutputType(Class<?> type) {
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        return this.outputType.isAssignableFrom(type);
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onInit();
            return this;
        }
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onStart();
            this.presetVariables.entrySet().forEach(e -> this.setVariable(e.getKey(), e.getValue().supply().get()));
            this.start = Instant.now();
            this.startNano = System.nanoTime();
            return this;
        }
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onFlush();
            this.presetVariables.clear();
        }
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onStop();
            this.stop = Instant.now();
            this.stopNano = System.nanoTime();
        }
        return this;
    }

    @Override
    public void setCode(int code) {
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        this.code = Objects.requireNonNull(code, "Code cannot be null");
    }

    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        return this.delegateContext.getBeanProviders();
    }

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        return this.delegateContext.getBeanProvider(name);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanDefinition<Bean> definition)
            throws DiException {
        return this.delegateContext.queryBean(provider, definition);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanDefinition<Bean> definition) throws DiException {
        return this.delegateContext.queryBean(definition);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanDefinition<Bean> definition) throws DiException {
        return this.delegateContext.queryBean(provider, definition);
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanDefinition<Bean> definition) throws DiException {
        return this.delegateContext.queryBeans(provider, definition);
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanDefinition<Bean> definition) throws DiException {
        return this.delegateContext.queryBeans(definition);
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanDefinition<Bean> definition) throws DiException {
        return this.delegateContext.queryBeans(provider, definition);
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        return this.delegateContext.getPropertyProviders();
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        return this.delegateContext.getPropertyProvider(name);
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException {
        return this.delegateContext.getProperty(provider, key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        return this.delegateContext.getProperty(key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String providerName, String key, Class<T> type) throws DiException {
        return this.delegateContext.getProperty(providerName, key, type);
    }

    @Override
    public void setProperty(String provider, String key, Object value) throws DiException {
        this.delegateContext.setProperty(provider, key, value);
    }

    @Override
    @Deprecated
    public <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args) throws DiException {
        return this.delegateContext.newChildContext(contextClass, args);
    }

    @Override
    @Deprecated
    public void registerChildContextFactory(IDiChildContextFactory<? extends IDiContext> factory) {
        this.delegateContext.registerChildContextFactory(factory);
    }

    @Override
    public <ChildContext extends IDiContext> Set<IDiChildContextFactory<ChildContext>> getChildContextFactories()
            throws DiException {
        return this.delegateContext.getChildContextFactories();
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        return this.delegateContext.resolve(elementType, element);
    }

    @Override
    public Set<Resolved> resolve(Executable method) throws DiException {
        return this.delegateContext.resolve(method);
    }

    @Override
    public void addResolver(Class<? extends Annotation> annotation, IElementResolver resolver) {
        this.delegateContext.addResolver(annotation, resolver);
    }

    @Override
    @Deprecated
    public IDiContext copy() throws CopyException {
        wrapLifecycle(this::ensureInitializedAndStarted, CopyException.class);
        return this;
    }

    @Override
    public void recordException(RuntimeExceptionRecord runtimeExceptionRecord) {
        this.recordedException.add(runtimeExceptionRecord);
    }

    @Override
    public Optional<RuntimeExceptionRecord> findException(RuntimeExceptionRecord pattern) {
        return this.recordedException.stream().filter(e -> e.matches(pattern)).findAny();
    }

    @Override
    public Optional<RuntimeExceptionRecord> findAbortingExceptionReport() {
        return this.recordedException.stream().filter(e -> e.hasAborted()).findAny();
    }

}
