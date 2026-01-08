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

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.utils.CopyException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeContext<InputType, OutputType> extends AbstractLifecycle
        implements IRuntimeContext<InputType, OutputType> {

    private final InputType input;
    @Getter
    private final Class<OutputType> outputType;
    private OutputType output;
    private final Map<String, ISupplier<?>> presetVariables = new HashMap<>();
    private Instant start;
    private Instant stop;
    private long startNano;
    private long stopNano;
    private final UUID uuid;
    private Integer code = IRuntime.GENERIC_RUNTIME_SUCCESS_CODE;
    private final IInjectionContext delegateContext;

    private final Object lifecycleMutex = new Object();
    private final Set<RuntimeExceptionRecord> recordedException = new HashSet<>();

    public RuntimeContext(IInjectionContext parent, InputType input, Class<OutputType> outputType,
            Map<String, ISupplier<?>> presetVariables, UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "Uuid cannot be null");
        log.atTrace().log(
                "[RuntimeContext.<init>] Entering constructor with parent={}, input={}, outputType={}, presetVariables={}",
                parent, input, outputType, presetVariables);
        this.delegateContext = Objects.requireNonNull(parent, "Parent context cannot be null");
        this.input = Objects.requireNonNull(input, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output type cannot be null");
        this.presetVariables
                .putAll(Map.copyOf(Objects.requireNonNull(presetVariables, "Preset variables map cannot be null")));
        log.atInfo().log("[RuntimeContext.<init>] RuntimeContext created with uuid={}", this.uuid);
    }

    @Override
    public IRuntimeResult<InputType, OutputType> getResult() {
        log.atTrace().log("[RuntimeContext.getResult] Entering getResult()");
        wrapLifecycle(this::ensureStopped, RuntimeException.class);
        wrapLifecycle(this::ensureNotFlushed, RuntimeException.class);

        IRuntimeResult<InputType, OutputType> result = new RuntimeResult<>(uuid, input, output, start, stop, startNano,
                stopNano, code, this.recordedException);
        log.atInfo().log("[RuntimeContext.getResult] Returning result with uuid={}, code={}", uuid, code);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <VariableType, InputType, OutputType> ISupplierBuilder<VariableType, IContextualSupplier<VariableType, IRuntimeContext<InputType, OutputType>>> variable(
            String variableName, Class<VariableType> variableType) {
        log.atTrace().log("[RuntimeContext.variable] Creating variable supplier for {} of type {}", variableName,
                variableType);
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getVariable(variableName, variableType);
        }, variableType, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<InputType, IContextualSupplier<InputType, IRuntimeContext<InputType, OutputType>>> input(
            Class<InputType> inputType) {
        log.atTrace().log("[RuntimeContext.input] Creating input supplier for type {}", inputType);
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getInput();
        }, inputType, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <ExceptionType extends Throwable, InputType, OutputType> ISupplierBuilder<ExceptionType, IContextualSupplier<ExceptionType, IRuntimeContext<InputType, OutputType>>> exception(
            Class<ExceptionType> exceptionType) {
        log.atTrace().log("[RuntimeContext.exception] Creating exception supplier for type {}", exceptionType);
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getException(exceptionType);
        }, exceptionType, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<Integer, IContextualSupplier<Integer, IRuntimeContext<InputType, OutputType>>> code() {
        log.atTrace().log("[RuntimeContext.code] Creating code supplier");
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getCode();
        }, Integer.class, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<String, IContextualSupplier<String, IRuntimeContext<InputType, OutputType>>> exceptionMessage() {
        log.atTrace().log("[RuntimeContext.exceptionMessage] Creating exceptionMessage supplier");
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getExceptionMessage();
        }, String.class, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<IRuntimeContext<InputType, OutputType>, IContextualSupplier<IRuntimeContext<InputType, OutputType>, IRuntimeContext<InputType, OutputType>>> context() {
        log.atTrace().log("[RuntimeContext.context] Creating context supplier");
        return new ContextualSupplierBuilder<>((context, others) -> {
            return Optional.of(context);
        }, (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class,
                (Class<IRuntimeContext<InputType, OutputType>>) (Class<?>) IRuntimeContext.class);
    }

    @Override
    public <VariableType> Optional<VariableType> getVariable(String variableName, Class<VariableType> variableType) {
        log.atTrace().log("[RuntimeContext.getVariable] Fetching variable '{}' of type {}", variableName, variableType);
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        Optional<VariableType> value = this.delegateContext
                .getProperty(Predefined.PropertyProviders.garganttua.toString(), variableName, variableType);
        log.atDebug().log("[RuntimeContext.getVariable] Fetched value={}", value);
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ExceptionType> Optional<ExceptionType> getException(Class<ExceptionType> exceptionType) {
        log.atTrace().log("[RuntimeContext.getException] Fetching exception of type {}", exceptionType);
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        Optional<RuntimeExceptionRecord> report = this.findAbortingExceptionReport();
        if (report.isPresent()) {
            if (exceptionType.isAssignableFrom(report.get().exceptionType())) {
                log.atDebug().log("[RuntimeContext.getException] Found exception record={}", report.get());
                return (Optional<ExceptionType>) Optional.of(report.get().exception());
            }
        }
        log.atDebug().log("[RuntimeContext.getException] No matching exception found");
        return Optional.empty();
    }

    @Override
    public Optional<InputType> getInput() {
        log.atTrace().log("[RuntimeContext.getInput] Retrieving input");
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        return Optional.of(this.input);
    }

    @Override
    public Optional<Integer> getCode() {
        log.atTrace().log("[RuntimeContext.getCode] Retrieving code");
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        return Optional.of(this.code);
    }

    @Override
    public Optional<String> getExceptionMessage() {
        log.atTrace().log("[RuntimeContext.getExceptionMessage] Retrieving exception message");
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);

        String message = null;
        Optional<RuntimeExceptionRecord> report = this.findAbortingExceptionReport();
        if (report.isPresent()) {
            message = report.get().exceptionMessage();
        }

        log.atDebug().log("[RuntimeContext.getExceptionMessage] Exception message={}", message);
        return Optional.ofNullable(message);
    }

    @Override
    public <VariableType> void setVariable(String variableName, VariableType variable) {
        log.atTrace().log("[RuntimeContext.setVariable] Setting variable '{}' to value={}", variableName, variable);
        wrapLifecycle(this::ensureInitialized, RuntimeException.class);
        this.delegateContext.setProperty(Predefined.PropertyProviders.garganttua.toString(), variableName,
                variable);
    }

    @Override
    public void setOutput(OutputType output) {
        log.atTrace().log("[RuntimeContext.setOutput] Setting output={}", output);
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        this.output = Objects.requireNonNull(output, "output cannot be null");
    }

    @Override
    public boolean isOfOutputType(Class<?> type) {
        log.atTrace().log("[RuntimeContext.isOfOutputType] Checking type {}", type);
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        return this.outputType.isAssignableFrom(type);
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        log.atTrace().log("[RuntimeContext.doInit] Initializing lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onInit();
            return this;
        }
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        log.atTrace().log("[RuntimeContext.doStart] Starting lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onStart();
            this.presetVariables.entrySet().forEach(e -> this.setVariable(e.getKey(), e.getValue().supply().get()));
            this.start = Instant.now();
            this.startNano = System.nanoTime();
            log.atInfo().log("[RuntimeContext.doStart] Lifecycle started at {} (nano={})", this.start, this.startNano);
            return this;
        }
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        log.atTrace().log("[RuntimeContext.doFlush] Flushing lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onFlush();
            this.presetVariables.clear();
            log.atInfo().log("[RuntimeContext.doFlush] Preset variables cleared");
        }
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        log.atTrace().log("[RuntimeContext.doStop] Stopping lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onStop();
            this.stop = Instant.now();
            this.stopNano = System.nanoTime();
            log.atInfo().log("[RuntimeContext.doStop] Lifecycle stopped at {} (nano={})", this.stop, this.stopNano);
        }
        return this;
    }

    @Override
    public void setCode(int code) {
        log.atTrace().log("[RuntimeContext.setCode] Setting code={}", code);
        wrapLifecycle(this::ensureInitializedAndStarted, RuntimeException.class);
        this.code = Objects.requireNonNull(code, "Code cannot be null");
    }

    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        log.atTrace().log("[RuntimeContext.getBeanProviders] Fetching bean providers");
        return this.delegateContext.getBeanProviders();
    }

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        log.atTrace().log("[RuntimeContext.getBeanProvider] Fetching bean provider for name={}", name);
        return this.delegateContext.getBeanProvider(name);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanReference<Bean> query)
            throws DiException {
        log.atTrace().log("[RuntimeContext.queryBean] Querying bean with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBean(provider, query);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanReference<Bean> query) throws DiException {
        log.atTrace().log("[RuntimeContext.queryBean] Querying bean with query={}", query);
        return this.delegateContext.queryBean(query);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanReference<Bean> query) throws DiException {
        log.atTrace().log("[RuntimeContext.queryBean] Querying bean with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBean(provider, query);
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanReference<Bean> query) throws DiException {
        log.atTrace().log("[RuntimeContext.queryBeans] Querying beans with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBeans(provider, query);
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanReference<Bean> query) throws DiException {
        log.atTrace().log("[RuntimeContext.queryBeans] Querying beans with query={}", query);
        return this.delegateContext.queryBeans(query);
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanReference<Bean> query) throws DiException {
        log.atTrace().log("[RuntimeContext.queryBeans] Querying beans with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBeans(provider, query);
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        log.atTrace().log("[RuntimeContext.getPropertyProviders] Fetching property providers");
        return this.delegateContext.getPropertyProviders();
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        log.atTrace().log("[RuntimeContext.getPropertyProvider] Fetching property provider for name={}", name);
        return this.delegateContext.getPropertyProvider(name);
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException {
        log.atTrace().log("[RuntimeContext.getProperty] Fetching property with provider={} key={} type={}", provider,
                key, type);
        return this.delegateContext.getProperty(provider, key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        log.atTrace().log("[RuntimeContext.getProperty] Fetching property with key={} type={}", key, type);
        return this.delegateContext.getProperty(key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String providerName, String key, Class<T> type) throws DiException {
        log.atTrace().log("[RuntimeContext.getProperty] Fetching property with provider={} key={} type={}",
                providerName, key, type);
        return this.delegateContext.getProperty(providerName, key, type);
    }

    @Override
    public void setProperty(String provider, String key, Object value) throws DiException {
        log.atTrace().log("[RuntimeContext.setProperty] Setting property with provider={} key={} value={}", provider,
                key, value);
        this.delegateContext.setProperty(provider, key, value);
    }

    @Override
    @Deprecated
    public <ChildContext extends IInjectionContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args) throws DiException {
        log.atTrace().log("[RuntimeContext.newChildContext] Creating new child context of class={} with args={}",
                contextClass, args);
        return this.delegateContext.newChildContext(contextClass, args);
    }

    @Override
    @Deprecated
    public void registerChildContextFactory(IInjectionChildContextFactory<? extends IInjectionContext> factory) {
        log.atTrace().log("[RuntimeContext.registerChildContextFactory] Registering child context factory {}", factory);
        this.delegateContext.registerChildContextFactory(factory);
    }

    @Override
    public <ChildContext extends IInjectionContext> Set<IInjectionChildContextFactory<ChildContext>> getChildContextFactories()
            throws DiException {
        log.atTrace().log("[RuntimeContext.getChildContextFactories] Fetching child context factories");
        return this.delegateContext.getChildContextFactories();
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        log.atTrace().log("[RuntimeContext.resolve] Resolving element {} of type {}", element, elementType);
        return this.delegateContext.resolve(elementType, element);
    }

    @Override
    public Set<Resolved> resolve(Executable method) throws DiException {
        log.atTrace().log("[RuntimeContext.resolve] Resolving method {}", method);
        return this.delegateContext.resolve(method);
    }

    @Override
    public void addResolver(Class<? extends Annotation> annotation, IElementResolver resolver) {
        log.atTrace().log("[RuntimeContext.addResolver] Adding resolver for annotation {}: {}", annotation, resolver);
        this.delegateContext.addResolver(annotation, resolver);
    }

    @Override
    @Deprecated
    public IInjectionContext copy() throws CopyException {
        log.atTrace().log("[RuntimeContext.copy] Copying context");
        wrapLifecycle(this::ensureInitializedAndStarted, CopyException.class);
        return this;
    }

    @Override
    public void recordException(RuntimeExceptionRecord runtimeExceptionRecord) {
        log.atTrace().log("[RuntimeContext.recordException] Recording exception {}", runtimeExceptionRecord);
        this.recordedException.add(runtimeExceptionRecord);
    }

    @Override
    public Optional<RuntimeExceptionRecord> findException(RuntimeExceptionRecord pattern) {
        log.atTrace().log("[RuntimeContext.findException] Searching exception matching {}", pattern);
        return this.recordedException.stream().filter(e -> e.matches(pattern)).findAny();
    }

    @Override
    public Optional<RuntimeExceptionRecord> findAbortingExceptionReport() {
        log.atTrace().log("[RuntimeContext.findAbortingExceptionReport] Searching for aborting exception report");
        return this.recordedException.stream().filter(e -> e.hasAborted()).findAny();
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
        return this.delegateContext.nativeConfiguration();
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, T bean, boolean autoDetect)
            throws DiException {
        this.delegateContext.addBean(provider, reference, bean, autoDetect);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, Optional<T> bean, boolean autoDetect)
            throws DiException {
        this.delegateContext.addBean(provider, reference, bean, autoDetect);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, T bean) throws DiException {
        this.delegateContext.addBean(provider, reference, bean);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, Optional<T> bean) throws DiException {
        this.delegateContext.addBean(provider, reference, bean);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference) throws DiException {
        this.delegateContext.addBean(provider, reference);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, boolean autoDetect) throws DiException {
        this.delegateContext.addBean(provider, reference, autoDetect);
    }
}