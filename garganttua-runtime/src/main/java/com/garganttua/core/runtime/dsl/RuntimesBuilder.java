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

import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.RuntimeContextFactory;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimesBuilder extends AbstractAutomaticDependentBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>
        implements IRuntimesBuilder {

    private final Set<String> packages = new HashSet<>();

    private static final String SOURCE_CONTEXT = "context";
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_REFLECTION = "reflection";

    private final Map<String, IRuntimeBuilder<?, ?>> manualRuntimeBuilders = new HashMap<>();
    private final Map<String, IRuntimeBuilder<?, ?>> contextRuntimeBuilders = new HashMap<>();
    private final Map<String, IRuntimeBuilder<?, ?>> reflexionRuntimeBuilders = new HashMap<>();

    private final MultiSourceCollector<String, IRuntimeBuilder<?, ?>> collector;

    private IInjectionContextBuilder injectionContextBuilder;

    private RuntimesBuilder() {
        super(Set.of(
                DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.AUTO_DETECT)));

        this.collector = new MultiSourceCollector<>();
        collector.source(new FixedSupplier<>(manualRuntimeBuilders), 0, SOURCE_MANUAL);
        collector.source(new FixedSupplier<>(contextRuntimeBuilders), 1, SOURCE_CONTEXT);
        collector.source(new FixedSupplier<>(reflexionRuntimeBuilders), 2, SOURCE_REFLECTION);

        log.atInfo().log("RuntimesBuilder initialized with phase-aware dependencies");
    }

    @Override
    public IRuntimesBuilder withPackage(String packageName) {
        log.atDebug().log("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IRuntimesBuilder withPackages(String[] packageNames) {
        log.atDebug().log("Adding {} packages", packageNames.length);
        Objects.requireNonNull(packageNames, "Package names cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String name,
            Class<InputType> inputType,
            Class<OutputType> outputType) {

        log.atTrace()
                .log("Entering runtime({}, {}, {}) method", name, inputType.getSimpleName(),
                        outputType.getSimpleName());
        Objects.requireNonNull(name, "Name cannot be null");

        log.atTrace()
                .log("Validated runtime input parameters", name);

        IRuntimeBuilder<InputType, OutputType> runtimeBuilder;
        if (!this.manualRuntimeBuilders.containsKey(name)) {
            runtimeBuilder = new RuntimeBuilder<>(this, name, inputType, outputType);
            this.manualRuntimeBuilders.put(name, runtimeBuilder);
            log.atInfo().log("Created new runtime builder {}", name);
        } else {
            runtimeBuilder = (IRuntimeBuilder<InputType, OutputType>) this.manualRuntimeBuilders.get(name);
            log.atDebug().log("Reusing existing runtime builder {}", name);
        }
        log.atTrace()
                .log("Exiting runtime() method");

        return runtimeBuilder;
    }

    @Override
    protected Map<String, IRuntime<?, ?>> doBuild() throws DslException {
        log.atTrace().log("Entering doBuild() method");
        log.atInfo().log("Building all runtimes");

        Map<String, IRuntime<?, ?>> result = this.collector.build().entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                e -> {
                    log.atDebug().log("Building individual runtime");
                    return e.getValue().provide(this.injectionContextBuilder).build();
                }));

        log.atTrace().log("Exiting doBuild() method");
        return result;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection() method");
        // Base auto-detection without dependencies - nothing to do here
        log.atTrace().log("Exiting doAutoDetection() method");
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        log.atTrace().log("Entering doAutoDetectionWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            List<?> definitions = context.queryBeans(
                    new BeanReference<>(null, Optional.empty(), Optional.empty(), Set.of(RuntimeDefinition.class)));
            log.atInfo().log("Auto-detecting runtimes from InjectionContext");
            definitions.forEach(this::createAutoDetectedFromInjectionContextRuntime);
        }
        log.atTrace().log("Exiting doAutoDetectionWithDependency() method");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPreBuildWithDependency() with dependency: {}", dependency);
        // Nothing to do in pre-build phase for InjectionContext dependency
        log.atTrace().log("Exiting doPreBuildWithDependency() method");
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPostBuildWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            registerBuiltObjectInContext(context, this.built);
        }

        log.atTrace().log("Exiting doPostBuildWithDependency() method");
    }

    @SuppressWarnings("unchecked")
    private void registerBuiltObjectInContext(IInjectionContext context, Map<String, IRuntime<?, ?>> result) {
        log.atDebug().log("Registering Map<String, IRuntime<?, ?>> as bean in InjectionContext");
        Optional<IBeanProvider> beanProvider = context
                .getBeanProvider(Predefined.BeanProviders.garganttua.toString());

        beanProvider.ifPresent(provider -> {
            BeanReference<Map<String, IRuntime<?, ?>>> beanRef = new BeanReference<>(
                    (Class<Map<String, IRuntime<?, ?>>>) (Class<?>) Map.class,
                    Optional.of(BeanStrategy.singleton),
                    Optional.of("Runtimes"),
                    Set.of());
            provider.add(beanRef, result);
            log.atInfo().log(
                    "Map<String, IRuntime<?, ?>> successfully registered as bean with {} runtimes with 'runtimes' name",
                    result.size());
        });

        result.entrySet().forEach(e -> beanProvider.ifPresent(provider -> {
            BeanReference<IRuntime<?, ?>> beanRef = new BeanReference<>(
                    (Class<IRuntime<?, ?>>) (Class<?>) IRuntime.class,
                    Optional.of(BeanStrategy.singleton),
                    Optional.of(e.getKey()),
                    Set.of(RuntimeDefinition.class));
            provider.add(beanRef, e.getValue());
            log.atInfo().log(
                    "IRuntime<?, ?> successfully registered as bean with '" + e.getKey() + "' name");
        }));
    }

    private void createAutoDetectedFromInjectionContextRuntime(Object runtimeDefinitionObject) {
        RuntimeDefinition runtimeDefinition = runtimeDefinitionObject.getClass()
                .getAnnotation(RuntimeDefinition.class);
        String runtimeName = Optional.ofNullable(runtimeDefinitionObject.getClass().getAnnotation(Named.class))
                .map(Named::value)
                .orElse(runtimeDefinitionObject.getClass().getSimpleName());

        Class<?> input = runtimeDefinition.input();
        Class<?> output = runtimeDefinition.output();

        log.atDebug()
                .log("Creating auto-detected runtime builder {} input={}, output={}", runtimeName,
                        input.getSimpleName(), output.getSimpleName());

        IRuntimeBuilder<?, ?> existingBuilder = this.manualRuntimeBuilders.remove(runtimeName);
        final IRuntimeBuilder<?, ?> runtimeBuilder;
        if (existingBuilder == null) {
            existingBuilder = new RuntimeBuilder<>(this, runtimeName, input, output,
                    runtimeDefinitionObject).autoDetect(true).provide(injectionContextBuilder);
        } else {
            ((RuntimeBuilder<?, ?>) existingBuilder)
                    .setObjectForAutoDetection(runtimeDefinitionObject).autoDetect(true)
                    .provide(injectionContextBuilder);
        }
        this.contextRuntimeBuilders.put(runtimeName, existingBuilder);
        log.atInfo().log("Auto-detected runtime {} registered", runtimeName);
    }

    public static IRuntimesBuilder builder() {
        log.atTrace().log("Entering builder() with no parameters");
        IRuntimesBuilder result = new RuntimesBuilder();
        log.atTrace().log("Exiting builder() with no parameters");
        return result;
    }

    @Override
    public IRuntimesBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder injectionContextBuilder) {
            this.injectionContextBuilder = injectionContextBuilder;
            this.setupInjectionContext(injectionContextBuilder);
        }
        return super.provide(dependency);
    }

    private IRuntimesBuilder setupInjectionContext(IInjectionContextBuilder context) {
        log.atTrace().log("Entering setupInjectionContext() method");

        Objects.requireNonNull(context, "Context builder cannot be null");

        if (!context.isAutoDetected()) {
            context.childContextFactory(new RuntimeContextFactory());
            context.resolvers().withResolver(Input.class, new InputElementResolver());
            context.resolvers().withResolver(Variable.class, new VariableElementResolver());
            context.resolvers().withResolver(Context.class, new ContextElementResolver());
            context.resolvers().withResolver(Exception.class, new ExceptionElementResolver());
            context.resolvers().withResolver(Code.class, new CodeElementResolver());
            context.resolvers().withResolver(ExceptionMessage.class, new ExceptionMessageElementResolver());

            log.atInfo().log("Context builder configured with resolvers");
        } else {
            context.withPackage("com.garganttua.core.runtime");
            log.atInfo().log("Context builder configured with packages for auto-detection");
        }

        log.atTrace().log("Exiting context() method");
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

}
