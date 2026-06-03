package com.garganttua.core.runtime.dsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.RuntimeContextFactory;
import com.garganttua.core.runtime.RuntimesRegistry;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Context;
import com.garganttua.core.runtime.annotations.Exception;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.runtime.resolver.CodeElementResolver;
import com.garganttua.core.runtime.resolver.ContextElementResolver;
import com.garganttua.core.runtime.resolver.ExceptionElementResolver;
import com.garganttua.core.runtime.resolver.ExceptionMessageElementResolver;
import com.garganttua.core.runtime.resolver.InputElementResolver;
import com.garganttua.core.runtime.resolver.VariableElementResolver;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Top-level builder that assembles the map of named {@link IRuntime} instances for
 * the application.
 *
 * <p>Collects runtime definitions from three sources — manual ({@link #runtime}),
 * injection-context auto-detection ({@code @RuntimeDefinition} beans) and
 * reflection scanning — wires the runtime parameter resolvers into the injection
 * context, builds all runtimes and registers them back as beans wrapped in a
 * {@link RuntimesRegistry}.</p>
 *
 * <p>Discoverable as a bootstrap builder via {@code @Bootstrap}.</p>
 */
@Bootstrap
@Reflected
@ConfigurableBuilder("runtimes")
public class RuntimesBuilder extends AbstractAutomaticDependentBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>
        implements IRuntimesBuilder {
    private static final Logger log = Logger.getLogger(RuntimesBuilder.class);

    private final Set<String> packages = new HashSet<>();

    private static final String SOURCE_CONTEXT = "context";
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_REFLECTION = "reflection";

    private final Map<String, IRuntimeBuilder<?, ?>> manualRuntimeBuilders = new HashMap<>();
    private final Map<String, IRuntimeBuilder<?, ?>> contextRuntimeBuilders = new HashMap<>();
    private final Map<String, IRuntimeBuilder<?, ?>> reflexionRuntimeBuilders = new HashMap<>();

    private final MultiSourceCollector<String, IRuntimeBuilder<?, ?>> collector;

    private IInjectionContextBuilder injectionContextBuilder;
    private IObservableBuilder<?, ?> reflectionBuilderRef;

    private RuntimesBuilder() {
        super(Set.of(
                DependencySpec.require(IClass.getClass(IInjectionContextBuilder.class), DependencyPhase.AUTO_DETECT),
                DependencySpec.use(IClass.getClass(IReflectionBuilder.class), DependencyPhase.BUILD)));

        this.collector = new MultiSourceCollector<>();
        @SuppressWarnings("unchecked")
        IClass<Map<String, IRuntimeBuilder<?, ?>>> mapType = (IClass<Map<String, IRuntimeBuilder<?, ?>>>) (IClass<?>) IClass.getClass(Map.class);
        collector.source(new FixedSupplier<>(manualRuntimeBuilders, mapType), 0, SOURCE_MANUAL);
        collector.source(new FixedSupplier<>(contextRuntimeBuilders, mapType), 1, SOURCE_CONTEXT);
        collector.source(new FixedSupplier<>(reflexionRuntimeBuilders, mapType), 2, SOURCE_REFLECTION);

        log.debug("RuntimesBuilder initialized with phase-aware dependencies");
    }

    /**
     * No-op build observer registration; this builder does not emit build events.
     *
     * @param observer the observer (ignored)
     * @return this builder for chaining
     */
    @Override
    public IRuntimesBuilder observer(com.garganttua.core.dsl.IBuilderObserver<IRuntimesBuilder, Map<String, IRuntime<?, ?>>> observer) {
        // IObservableBuilder contract — build-time callback. Not used by
        // this builder today (no observable build-event fan-out), kept as
        // a no-op for interface completeness.
        return this;
    }

    /**
     * Adds a package to scan for {@code @RuntimeDefinition} classes.
     *
     * @param packageName the package name to add
     * @return this builder for chaining
     */
    @Override
    public IRuntimesBuilder withPackage(String packageName) {
        log.debug("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    /**
     * Adds several packages to scan for {@code @RuntimeDefinition} classes.
     *
     * @param packageNames the package names to add
     * @return this builder for chaining
     */
    @Override
    public IRuntimesBuilder withPackages(String[] packageNames) {
        log.debug("Adding {} packages", packageNames.length);
        Objects.requireNonNull(packageNames, "Package names cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        return this;
    }

    /**
     * Returns a manual runtime builder for the given name, creating it on first
     * use and reusing it on subsequent calls.
     *
     * @param name       the unique runtime name
     * @param inputType  the runtime input type
     * @param outputType the runtime output type
     * @return the runtime builder for further configuration
     */
    @SuppressWarnings("unchecked")
    @Override
    public <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String name,
            IClass<InputType> inputType,
            IClass<OutputType> outputType) {

        log.trace("Entering runtime({}, {}, {}) method", name, inputType.getSimpleName(),
                        outputType.getSimpleName());
        Objects.requireNonNull(name, "Name cannot be null");

        log.trace("Validated runtime input parameters", name);

        IRuntimeBuilder<InputType, OutputType> runtimeBuilder;
        if (!this.manualRuntimeBuilders.containsKey(name)) {
            runtimeBuilder = new RuntimeBuilder<>(this, name, (Class<InputType>) inputType.getType(), (Class<OutputType>) outputType.getType());
            this.manualRuntimeBuilders.put(name, runtimeBuilder);
            log.debug("Created new runtime builder {}", name);
        } else {
            runtimeBuilder = (IRuntimeBuilder<InputType, OutputType>) this.manualRuntimeBuilders.get(name);
            log.debug("Reusing existing runtime builder {}", name);
        }
        log.trace("Exiting runtime() method");

        return runtimeBuilder;
    }

    @Override
    protected Map<String, IRuntime<?, ?>> doBuild() throws DslException {
        log.trace("Entering doBuild() method");
        log.debug("Building all runtimes");

        Map<String, IRuntime<?, ?>> runtimesMap = this.collector.build().entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                e -> {
                    log.debug("Building individual runtime");
                    IRuntimeBuilder<?, ?> rb = e.getValue();
                    if (this.reflectionBuilderRef != null) {
                        rb.provide(this.reflectionBuilderRef);
                    }
                    return rb.provide(this.injectionContextBuilder).build();
                }));

        // Wrap in RuntimesRegistry to provide summary information
        RuntimesRegistry result = new RuntimesRegistry(runtimesMap);

        log.trace("Exiting doBuild() method");
        return result;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("Entering doAutoDetection() method");
        // Base auto-detection without dependencies - nothing to do here
        log.trace("Exiting doAutoDetection() method");
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        log.trace("Entering doAutoDetectionWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            List<?> definitions = context.queryBeans(
                    new BeanReference<>(null, Optional.empty(), Optional.empty(), Set.of(IClass.getClass(RuntimeDefinition.class))));
            log.debug("Auto-detecting runtimes from InjectionContext");
            definitions.forEach(this::createAutoDetectedFromInjectionContextRuntime);
        }
        log.trace("Exiting doAutoDetectionWithDependency() method");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        log.trace("Entering doPreBuildWithDependency() with dependency: {}", dependency);
        // Nothing to do in pre-build phase for InjectionContext dependency
        log.trace("Exiting doPreBuildWithDependency() method");
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        log.trace("Entering doPostBuildWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            registerBuiltObjectInContext(context, this.built);
        }

        log.trace("Exiting doPostBuildWithDependency() method");
    }

    @SuppressWarnings("unchecked")
    private void registerBuiltObjectInContext(IInjectionContext context, Map<String, IRuntime<?, ?>> result) {
        log.debug("Registering Map<String, IRuntime<?, ?>> as bean in InjectionContext");
        String providerName = Predefined.BeanProviders.garganttua.toString();

        // Use addBean directly to avoid lifecycle check - the context may not be started yet
        // during Bootstrap's build phase
        BeanReference<Map<String, IRuntime<?, ?>>> mapBeanRef = new BeanReference<>(
                (IClass<Map<String, IRuntime<?, ?>>>) (IClass<?>) IClass.getClass(Map.class),
                Optional.of(BeanStrategy.singleton),
                Optional.of("Runtimes"),
                Set.of());
        context.addBean(providerName, mapBeanRef, result);
        log.debug(
                "Map<String, IRuntime<?, ?>> successfully registered as bean with {} runtimes with 'runtimes' name",
                result.size());

        result.entrySet().forEach(e -> {
            BeanReference<IRuntime<?, ?>> beanRef = new BeanReference<>(
                    (IClass<IRuntime<?, ?>>) (IClass<?>) IClass.getClass(IRuntime.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.of(e.getKey()),
                    Set.of(IClass.getClass(RuntimeDefinition.class)));
            context.addBean(providerName, beanRef, e.getValue());
            log.debug(
                    "IRuntime<?, ?> successfully registered as bean with '" + e.getKey() + "' name");
        });
    }

    private void createAutoDetectedFromInjectionContextRuntime(Object runtimeDefinitionObject) {
        RuntimeDefinition runtimeDefinition = runtimeDefinitionObject.getClass()
                .getAnnotation(RuntimeDefinition.class);
        String runtimeName = Optional.ofNullable(runtimeDefinitionObject.getClass().getAnnotation(Named.class))
                .map(Named::value)
                .orElse(runtimeDefinitionObject.getClass().getSimpleName());

        Class<?> input = runtimeDefinition.input();
        Class<?> output = runtimeDefinition.output();

        log.debug("Creating auto-detected runtime builder {} input={}, output={}", runtimeName,
                        input.getSimpleName(), output.getSimpleName());

        IRuntimeBuilder<?, ?> existingBuilder = this.manualRuntimeBuilders.remove(runtimeName);
        final IRuntimeBuilder<?, ?> runtimeBuilder;
        if (existingBuilder == null) {
            RuntimeBuilder<?, ?> newBuilder = new RuntimeBuilder<>(this, runtimeName, input, output,
                    runtimeDefinitionObject);
            if (this.reflectionBuilderRef != null) {
                newBuilder.provide(this.reflectionBuilderRef);
            }
            existingBuilder = newBuilder.autoDetect(true).provide(injectionContextBuilder);
        } else {
            RuntimeBuilder<?, ?> rb = (RuntimeBuilder<?, ?>) existingBuilder;
            if (this.reflectionBuilderRef != null) {
                rb.provide(this.reflectionBuilderRef);
            }
            rb.setObjectForAutoDetection(runtimeDefinitionObject).autoDetect(true)
                    .provide(injectionContextBuilder);
        }
        this.contextRuntimeBuilders.put(runtimeName, existingBuilder);
        log.debug("Auto-detected runtime {} registered", runtimeName);
    }

    /**
     * Creates a new, empty {@link RuntimesBuilder}.
     *
     * @return a fresh runtimes builder
     */
    public static IRuntimesBuilder builder() {
        log.trace("Entering builder() with no parameters");
        IRuntimesBuilder result = new RuntimesBuilder();
        log.trace("Exiting builder() with no parameters");
        return result;
    }

    /**
     * Receives a build dependency, capturing the injection context (and wiring its
     * runtime resolvers) and reflection builder when present, then delegates to the
     * superclass.
     *
     * @param dependency the dependency builder being provided
     * @return this builder for chaining
     * @throws DslException if the superclass rejects the dependency
     */
    @Override
    public IRuntimesBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder injectionContextBuilder) {
            this.injectionContextBuilder = injectionContextBuilder;
            this.setupInjectionContext(injectionContextBuilder);
        }
        if (dependency instanceof IReflectionBuilder) {
            this.reflectionBuilderRef = dependency;
        }
        return super.provide(dependency);
    }

    private IRuntimesBuilder setupInjectionContext(IInjectionContextBuilder context) {
        log.trace("Entering setupInjectionContext() method");

        Objects.requireNonNull(context, "Context builder cannot be null");

        if (!context.isAutoDetected()) {
            context.childContextFactory(new RuntimeContextFactory());
            context.resolvers().withResolver(IClass.getClass(Input.class), new InputElementResolver());
            context.resolvers().withResolver(IClass.getClass(Variable.class), new VariableElementResolver());
            context.resolvers().withResolver(IClass.getClass(Context.class), new ContextElementResolver());
            context.resolvers().withResolver(IClass.getClass(Exception.class), new ExceptionElementResolver());
            context.resolvers().withResolver(IClass.getClass(Code.class), new CodeElementResolver());
            context.resolvers().withResolver(IClass.getClass(ExceptionMessage.class), new ExceptionMessageElementResolver());

            log.debug("Context builder configured with resolvers");
        } else {
            context.withPackage("com.garganttua.core.runtime");
            log.debug("Context builder configured with packages for auto-detection");
        }

        log.trace("Exiting context() method");
        return this;
    }

    /**
     * Returns the packages registered for {@code @RuntimeDefinition} scanning.
     *
     * @return the configured package names
     */
    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

}
