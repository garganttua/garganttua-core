package com.garganttua.core.runtime;

import java.lang.annotation.Annotation;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IExecutable;
import java.time.Instant;
import java.util.Collections;
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
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.utils.CopyException;

/**
 * Runtime execution context — JVM implementation of {@link IRuntimeContext}.
 *
 * <p><b>Size note:</b> this class exceeds the 500-line advisory threshold; it is a
 * cohesive {@code IRuntimeContext} mirror (input/output/variable/context accessors plus
 * lifecycle) with no extractable multi-responsibility cluster. Documented exception per
 * the alpha02 rework rules, alongside {@code RuntimeClass} and {@code AOTClass}.</p>
 */
public class RuntimeContext<InputType, OutputType> extends AbstractLifecycle
        implements IRuntimeContext<InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeContext.class);

    private final InputType input;
    private final IClass<?> outputType;
    private OutputType output;
    private final Map<String, ISupplier<?>> presetVariables = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Object> variables = Collections.synchronizedMap(new HashMap<>());
    private Instant start;
    private Instant stop;
    private long startNano;
    private long stopNano;
    private final UUID uuid;
    private Integer code = IRuntime.GENERIC_RUNTIME_SUCCESS_CODE;
    private final IInjectionContext delegateContext;

    private final Object lifecycleMutex = new Object();
    private final Set<RuntimeExceptionRecord> recordedException = Collections.synchronizedSet(new HashSet<>());

    /**
     * Creates a runtime context backed by the given parent injection context.
     *
     * @param parent          the delegate injection context (bean/property resolution)
     * @param input           the runtime input value
     * @param outputType      the declared output type, used for output validation
     * @param presetVariables variables seeded into the context on start
     * @param uuid            the execution correlation id
     */
    public RuntimeContext(IInjectionContext parent, InputType input, Class<OutputType> outputType,
            Map<String, ISupplier<?>> presetVariables, UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "Uuid cannot be null");
        log.trace(
                "[RuntimeContext.<init>] Entering constructor with parent={}, input={}, outputType={}, presetVariables={}",
                parent, input, outputType, presetVariables);
        this.delegateContext = Objects.requireNonNull(parent, "Parent context cannot be null");
        this.input = Objects.requireNonNull(input, "Input type cannot be null");
        this.outputType = IClass.getClass(Objects.requireNonNull(outputType, "Output type cannot be null"));
        this.presetVariables
                .putAll(Map.copyOf(Objects.requireNonNull(presetVariables, "Preset variables map cannot be null")));
        log.debug("[RuntimeContext.<init>] RuntimeContext created with uuid={}", this.uuid);
    }

    @Override
    public IRuntimeResult<InputType, OutputType> getResult() {
        log.trace("[RuntimeContext.getResult] Entering getResult()");
        wrapLifecycle(this::ensureStopped, IClass.getClass(RuntimeException.class));
        wrapLifecycle(this::ensureNotFlushed, IClass.getClass(RuntimeException.class));

        Set<RuntimeExceptionRecord> exceptionsCopy;
        synchronized (this.recordedException) {
            exceptionsCopy = Set.copyOf(this.recordedException);
        }
        IRuntimeResult<InputType, OutputType> result = new RuntimeResult<>(uuid, input, output, start, stop, startNano,
                stopNano, code, exceptionsCopy, Map.copyOf(this.variables));
        log.debug("[RuntimeContext.getResult] Returning result with uuid={}, code={}", uuid, code);
        return result;
    }

    /**
     * Builds a contextual supplier that resolves a named variable from the runtime context.
     *
     * @param variableName the variable name to read
     * @param variableType the expected variable type
     * @return a supplier builder yielding the variable value
     */
    @SuppressWarnings("unchecked")
    public static <VariableType, InputType, OutputType> ISupplierBuilder<VariableType, IContextualSupplier<VariableType, IRuntimeContext<InputType, OutputType>>> variable(
            String variableName, IClass<VariableType> variableType) {
        log.trace("[RuntimeContext.variable] Creating variable supplier for {} of type {}", variableName,
                variableType);
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getVariable(variableName, variableType);
        }, variableType, (IClass<IRuntimeContext<InputType, OutputType>>) (IClass<?>) IClass.getClass(IRuntimeContext.class));
    }

    /**
     * Builds a contextual supplier that resolves the runtime input.
     *
     * @param inputType the expected input type
     * @return a supplier builder yielding the input value
     */
    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<InputType, IContextualSupplier<InputType, IRuntimeContext<InputType, OutputType>>> input(
            IClass<InputType> inputType) {
        log.trace("[RuntimeContext.input] Creating input supplier for type {}", inputType);
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getInput();
        }, inputType, (IClass<IRuntimeContext<InputType, OutputType>>) (IClass<?>) IClass.getClass(IRuntimeContext.class));
    }

    /**
     * Builds a contextual supplier that resolves a recorded exception of the given type.
     *
     * @param exceptionType the expected exception type
     * @return a supplier builder yielding the matching exception
     */
    @SuppressWarnings("unchecked")
    public static <ExceptionType extends Throwable, InputType, OutputType> ISupplierBuilder<ExceptionType, IContextualSupplier<ExceptionType, IRuntimeContext<InputType, OutputType>>> exception(
            IClass<ExceptionType> exceptionType) {
        log.trace("[RuntimeContext.exception] Creating exception supplier for type {}", exceptionType);
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getException(exceptionType);
        }, exceptionType, (IClass<IRuntimeContext<InputType, OutputType>>) (IClass<?>) IClass.getClass(IRuntimeContext.class));
    }

    /**
     * Builds a contextual supplier that resolves the current runtime exit code.
     *
     * @return a supplier builder yielding the code
     */
    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<Integer, IContextualSupplier<Integer, IRuntimeContext<InputType, OutputType>>> code() {
        log.trace("[RuntimeContext.code] Creating code supplier");
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getCode();
        }, IClass.getClass(Integer.class), (IClass<IRuntimeContext<InputType, OutputType>>) (IClass<?>) IClass.getClass(IRuntimeContext.class));
    }

    /**
     * Builds a contextual supplier that resolves the aborting exception's message.
     *
     * @return a supplier builder yielding the exception message
     */
    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<String, IContextualSupplier<String, IRuntimeContext<InputType, OutputType>>> exceptionMessage() {
        log.trace("[RuntimeContext.exceptionMessage] Creating exceptionMessage supplier");
        return new ContextualSupplierBuilder<>((context, others) -> {
            return context.getExceptionMessage();
        }, IClass.getClass(String.class), (IClass<IRuntimeContext<InputType, OutputType>>) (IClass<?>) IClass.getClass(IRuntimeContext.class));
    }

    /**
     * Builds a contextual supplier that yields the runtime context itself.
     *
     * @return a supplier builder yielding the current context
     */
    @SuppressWarnings("unchecked")
    public static <InputType, OutputType> ISupplierBuilder<IRuntimeContext<InputType, OutputType>, IContextualSupplier<IRuntimeContext<InputType, OutputType>, IRuntimeContext<InputType, OutputType>>> context() {
        log.trace("[RuntimeContext.context] Creating context supplier");
        return new ContextualSupplierBuilder<>((context, others) -> {
            return Optional.of(context);
        }, (IClass<IRuntimeContext<InputType, OutputType>>) (IClass<?>) IClass.getClass(IRuntimeContext.class),
                (IClass<IRuntimeContext<InputType, OutputType>>) (IClass<?>) IClass.getClass(IRuntimeContext.class));
    }

    @Override
    public <VariableType> Optional<VariableType> getVariable(String variableName, IClass<VariableType> variableType) {
        log.trace("[RuntimeContext.getVariable] Fetching variable '{}' of type {}", variableName, variableType);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        Optional<VariableType> value = this.delegateContext
                .getProperty(Predefined.PropertyProviders.garganttua.toString(), variableName, variableType);
        log.debug("[RuntimeContext.getVariable] Fetched value={}", value);
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ExceptionType> Optional<ExceptionType> getException(IClass<ExceptionType> exceptionType) {
        log.trace("[RuntimeContext.getException] Fetching exception of type {}", exceptionType);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        Optional<RuntimeExceptionRecord> report = this.findAbortingExceptionReport();
        if (report.isPresent()) {
            if (exceptionType.isAssignableFrom(report.get().exceptionType())) {
                log.debug("[RuntimeContext.getException] Found exception record={}", report.get());
                return (Optional<ExceptionType>) Optional.of(report.get().exception());
            }
        }
        log.debug("[RuntimeContext.getException] No matching exception found");
        return Optional.empty();
    }

    @Override
    public Optional<InputType> getInput() {
        log.trace("[RuntimeContext.getInput] Retrieving input");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        return Optional.of(this.input);
    }

    @Override
    public Optional<Integer> getCode() {
        log.trace("[RuntimeContext.getCode] Retrieving code");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        return Optional.of(this.code);
    }

    @Override
    public Optional<String> getExceptionMessage() {
        log.trace("[RuntimeContext.getExceptionMessage] Retrieving exception message");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));

        String message = null;
        Optional<RuntimeExceptionRecord> report = this.findAbortingExceptionReport();
        if (report.isPresent()) {
            message = report.get().exceptionMessage();
        }

        log.debug("[RuntimeContext.getExceptionMessage] Exception message={}", message);
        return Optional.ofNullable(message);
    }

    @Override
    public <VariableType> void setVariable(String variableName, VariableType variable) {
        log.trace("[RuntimeContext.setVariable] Setting variable '{}' to value={}", variableName, variable);
        wrapLifecycle(this::ensureInitialized, IClass.getClass(RuntimeException.class));
        this.delegateContext.setProperty(Predefined.PropertyProviders.garganttua.toString(), variableName,
                variable);
        this.variables.put(variableName, variable);
    }

    @Override
    public OutputType getOutput() {
        log.trace("[RuntimeContext.getOutput] Getting current output");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        return this.output;
    }

    @Override
    public void setOutput(OutputType output) {
        log.trace("[RuntimeContext.setOutput] Setting output={}", output);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        this.output = output;
    }

    @Override
    public IClass<?> getOutputType() {
        return this.outputType;
    }

    @Override
    public boolean isOfOutputType(IClass<?> type) {
        log.trace("[RuntimeContext.isOfOutputType] Checking type {}", type);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        try {
            return this.outputType.isAssignableFrom(type);
        } catch (RuntimeException e) {
            // Output-type validation is a safety net, not business logic. When a
            // type can't be resolved — notably in a native image where the class
            // isn't registered for reflection (Class.forName → "Cannot resolve
            // class") — don't abort the whole step: warn and accept the produced
            // value. Registering the type (@Reflected) restores strict checking.
            log.warn("[RuntimeContext.isOfOutputType] Could not resolve output type {} vs {} "
                    + "(accepting value, strict check skipped): {}",
                    this.outputType, type, e.getMessage());
            return true;
        }
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        log.trace("[RuntimeContext.doInit] Initializing lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onInit();
            return this;
        }
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        log.trace("[RuntimeContext.doStart] Starting lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onStart();
            this.presetVariables.entrySet().forEach(e -> this.setVariable(e.getKey(), e.getValue().supply().get()));
            this.start = Instant.now();
            this.startNano = System.nanoTime();
            log.debug("[RuntimeContext.doStart] Lifecycle started at {} (nano={})", this.start, this.startNano);
            return this;
        }
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        log.trace("[RuntimeContext.doFlush] Flushing lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onFlush();
            this.presetVariables.clear();
            log.debug("[RuntimeContext.doFlush] Preset variables cleared");
        }
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        log.trace("[RuntimeContext.doStop] Stopping lifecycle");
        synchronized (this.lifecycleMutex) {
            this.delegateContext.onStop();
            this.stop = Instant.now();
            this.stopNano = System.nanoTime();
            log.debug("[RuntimeContext.doStop] Lifecycle stopped at {} (nano={})", this.stop, this.stopNano);
        }
        return this;
    }

    @Override
    public void setCode(int code) {
        log.trace("[RuntimeContext.setCode] Setting code={}", code);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(RuntimeException.class));
        this.code = Objects.requireNonNull(code, "Code cannot be null");
    }

    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        log.trace("[RuntimeContext.getBeanProviders] Fetching bean providers");
        return this.delegateContext.getBeanProviders();
    }

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        log.trace("[RuntimeContext.getBeanProvider] Fetching bean provider for name={}", name);
        return this.delegateContext.getBeanProvider(name);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanReference<Bean> query)
            throws DiException {
        log.trace("[RuntimeContext.queryBean] Querying bean with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBean(provider, query);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanReference<Bean> query) throws DiException {
        log.trace("[RuntimeContext.queryBean] Querying bean with query={}", query);
        return this.delegateContext.queryBean(query);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanReference<Bean> query) throws DiException {
        log.trace("[RuntimeContext.queryBean] Querying bean with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBean(provider, query);
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanReference<Bean> query) throws DiException {
        log.trace("[RuntimeContext.queryBeans] Querying beans with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBeans(provider, query);
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanReference<Bean> query) throws DiException {
        log.trace("[RuntimeContext.queryBeans] Querying beans with query={}", query);
        return this.delegateContext.queryBeans(query);
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanReference<Bean> query) throws DiException {
        log.trace("[RuntimeContext.queryBeans] Querying beans with provider={} query={}", provider,
                query);
        return this.delegateContext.queryBeans(provider, query);
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        log.trace("[RuntimeContext.getPropertyProviders] Fetching property providers");
        return this.delegateContext.getPropertyProviders();
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        log.trace("[RuntimeContext.getPropertyProvider] Fetching property provider for name={}", name);
        return this.delegateContext.getPropertyProvider(name);
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, IClass<T> type) throws DiException {
        log.trace("[RuntimeContext.getProperty] Fetching property with provider={} key={} type={}", provider,
                key, type);
        return this.delegateContext.getProperty(provider, key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String key, IClass<T> type) throws DiException {
        log.trace("[RuntimeContext.getProperty] Fetching property with key={} type={}", key, type);
        return this.delegateContext.getProperty(key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String providerName, String key, IClass<T> type) throws DiException {
        log.trace("[RuntimeContext.getProperty] Fetching property with provider={} key={} type={}",
                providerName, key, type);
        return this.delegateContext.getProperty(providerName, key, type);
    }

    @Override
    public void setProperty(String provider, String key, Object value) throws DiException {
        log.trace("[RuntimeContext.setProperty] Setting property with provider={} key={} value={}", provider,
                key, value);
        this.delegateContext.setProperty(provider, key, value);
    }

    @Override
    public <ChildContext extends IInjectionContext> ChildContext newChildContext(IClass<ChildContext> contextClass,
            Object... args) throws DiException {
        log.trace("[RuntimeContext.newChildContext] Creating new child context of class={} with args={}",
                contextClass, args);
        return this.delegateContext.newChildContext(contextClass, args);
    }

    @Override
    public void registerChildContextFactory(IInjectionChildContextFactory<? extends IInjectionContext> factory) {
        log.trace("[RuntimeContext.registerChildContextFactory] Registering child context factory {}", factory);
        this.delegateContext.registerChildContextFactory(factory);
    }

    @Override
    public <ChildContext extends IInjectionContext> Set<IInjectionChildContextFactory<ChildContext>> getChildContextFactories()
            throws DiException {
        log.trace("[RuntimeContext.getChildContextFactories] Fetching child context factories");
        return this.delegateContext.getChildContextFactories();
    }

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {
        log.trace("[RuntimeContext.resolve] Resolving element {} of type {}", element, elementType);
        return this.delegateContext.resolve(elementType, element);
    }

    @Override
    public Set<Resolved> resolve(IExecutable method) throws DiException {
        log.trace("[RuntimeContext.resolve] Resolving method {}", method);
        return this.delegateContext.resolve(method);
    }

    @Override
    public void addResolver(IClass<? extends Annotation> annotation, IElementResolver resolver) {
        log.trace("[RuntimeContext.addResolver] Adding resolver for annotation {}: {}", annotation, resolver);
        this.delegateContext.addResolver(annotation, resolver);
    }

    @Override
    @Deprecated
    public IInjectionContext copy() throws CopyException {
        log.trace("[RuntimeContext.copy] Copying context");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(CopyException.class));
        return this;
    }

    @Override
    public void recordException(RuntimeExceptionRecord runtimeExceptionRecord) {
        log.trace("[RuntimeContext.recordException] Recording exception {}", runtimeExceptionRecord);
        this.recordedException.add(runtimeExceptionRecord);
    }

    @Override
    public Optional<RuntimeExceptionRecord> findException(RuntimeExceptionRecord pattern) {
        log.trace("[RuntimeContext.findException] Searching exception matching {}", pattern);
        synchronized (this.recordedException) {
            return this.recordedException.stream().filter(e -> e.matches(pattern)).findAny();
        }
    }

    @Override
    public Optional<RuntimeExceptionRecord> findAbortingExceptionReport() {
        log.trace("[RuntimeContext.findAbortingExceptionReport] Searching for aborting exception report");
        synchronized (this.recordedException) {
            return this.recordedException.stream().filter(e -> e.hasAborted()).findAny();
        }
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public Set<IReflectionConfigurationEntryBuilder> reflectionUsage() {
        return this.delegateContext.reflectionUsage();
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

    @Override
    public IReflection reflection() {
        return IClass.getReflection();
    }
}
