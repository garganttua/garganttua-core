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

import com.garganttua.core.dsl.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.DslException;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimesBuilder extends AbstractAutomaticDependentBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>
        implements IRuntimesBuilder {

    private final Map<String, IRuntimeBuilder<?, ?>> runtimeBuilders = new HashMap<>();
    private final Set<String> packages = new HashSet<>();
    private IInjectionContextBuilder contextBuilder;

    private RuntimesBuilder(Optional<IInjectionContextBuilder> contextBuilder) {
        super(
            contextBuilder.isPresent() ? Set.of(IInjectionContextBuilder.class) : Set.of(),
            Set.of()
        );

        contextBuilder.ifPresent(builder -> {
            this.contextBuilder = builder;
            this.provide(builder);
            builder.withPackage("com.garganttua.core.runtime.annotations");
            builder.childContextFactory(new RuntimeContextFactory());
        });

        log.atInfo().log("RuntimesBuilder initialized");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String name,
            Class<InputType> inputType,
            Class<OutputType> outputType) {

        log.atTrace()
                .log("Entering runtime({}, {}, {}) method", name, inputType.getSimpleName(),
                        outputType.getSimpleName());

        // Require context builder to be provided
        if (this.contextBuilder == null) {
            throw new IllegalStateException("Context builder is required for runtime operations");
        }

        Objects.requireNonNull(name, "Name cannot be null");

        log.atTrace()
                .log("Validated runtime input parameters", name);

        IRuntimeBuilder<InputType, OutputType> runtimeBuilder;
        if (!this.runtimeBuilders.containsKey(name)) {
            runtimeBuilder = new RuntimeBuilder<>(this, name, inputType, outputType);
            this.runtimeBuilders.put(name, runtimeBuilder);
            log.atInfo().log("Created new runtime builder {}", name);
        } else {
            runtimeBuilder = (IRuntimeBuilder<InputType, OutputType>) this.runtimeBuilders.get(name);
            log.atDebug().log("Reusing existing runtime builder {}", name);
        }

        // If we have a built context, handle it now
        useDependencies.stream()
            .filter(dep -> dep.getDependency().equals(IInjectionContextBuilder.class))
            .filter(dep -> dep.isReady())
            .findFirst()
            .ifPresent(dep -> {
                IInjectionContext context = (IInjectionContext) dep.get();
                runtimeBuilder.handle(context);
                log.atDebug().log("Runtime builder {} handled with context", name);
            });

        log.atTrace()
                .log("Exiting runtime() method");

        return runtimeBuilder;
    }

    @Override
    protected Map<String, IRuntime<?, ?>> doBuild() throws DslException {
        log.atTrace().log("Entering doBuild() method");

        log.atInfo().log("Building all runtimes");

        Map<String, IRuntime<?, ?>> result = this.runtimeBuilders.entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                e -> {
                    log.atDebug().log("Building individual runtime");
                    return e.getValue().build();
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
            // Synchronize packages from InjectionContextBuilder before scanning
            synchronizePackagesFromContext();

            List<?> definitions = context.queryBeans(
                    new BeanReference<>(null, Optional.empty(), Optional.empty(), Set.of(RuntimeDefinition.class)));

            log.atInfo().log("Auto-detecting runtimes from InjectionContext");

            definitions.forEach(this::createAutoDetectedRuntime);
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

        result.entrySet().forEach(e ->
            beanProvider.ifPresent(provider -> {
                BeanReference<IRuntime<?,?>> beanRef = new BeanReference<>(
                        (Class<IRuntime<?, ?>>) (Class<?>) IRuntime.class,
                        Optional.of(BeanStrategy.singleton),
                        Optional.of(e.getKey()),
                        Set.of(RuntimeDefinition.class));
                provider.add(beanRef, e.getValue());
                log.atInfo().log(
                        "IRuntime<?, ?> successfully registered as bean with '"+e.getKey()+"' name");
            })
        );
    }

    /**
     * Synchronizes packages from the InjectionContextBuilder to this builder's packages.
     * This ensures that packages declared in the DI context are also used for runtime scanning.
     */
    private void synchronizePackagesFromContext() {
        log.atTrace().log("Entering synchronizePackagesFromContext()");

        useDependencies.stream()
            .filter(dep -> dep.getDependency().equals(IInjectionContextBuilder.class))
            .findFirst()
            .ifPresent(dep -> dep.synchronizePackagesFromContext(contextPackages -> {
                int beforeSize = this.packages.size();
                this.packages.addAll(contextPackages);
                int addedCount = this.packages.size() - beforeSize;
                if (addedCount > 0) {
                    log.atDebug().log("Synchronized {} new packages from InjectionContextBuilder", addedCount);
                }
            }));

        log.atTrace().log("Exiting synchronizePackagesFromContext()");
    }

    private void createAutoDetectedRuntime(Object runtimeDefinitionObject) {
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

        IRuntimeBuilder<?, ?> existingBuilder = this.runtimeBuilders.get(runtimeName);
        final IRuntimeBuilder<?, ?> runtimeBuilder;
        if (existingBuilder == null) {
            runtimeBuilder = new RuntimeBuilder<>(this, runtimeName, input, output,
                    runtimeDefinitionObject).autoDetect(true);
            this.runtimeBuilders.put(runtimeName, runtimeBuilder);
        } else {
            runtimeBuilder = ((RuntimeBuilder<?, ?>) existingBuilder)
                    .setObjectForAutoDetection(runtimeDefinitionObject).autoDetect(true);
        }

        // Handle with context if available
        useDependencies.stream()
            .filter(dep -> dep.getDependency().equals(IInjectionContextBuilder.class))
            .filter(dep -> dep.isReady())
            .findFirst()
            .ifPresent(dep -> {
                IInjectionContext context = (IInjectionContext) dep.get();
                runtimeBuilder.handle(context);
            });

        log.atInfo().log("Auto-detected runtime {} registered", runtimeName);
    }

    public static IRuntimesBuilder builder() {
        log.atTrace().log("Entering builder() with no parameters");
        IRuntimesBuilder result = new RuntimesBuilder(Optional.empty());
        log.atTrace().log("Exiting builder() with no parameters");
        return result;
    }

    public static IRuntimesBuilder builder(IInjectionContextBuilder contextBuilder) {
        log.atTrace().log("Entering builder() with IInjectionContextBuilder parameter");
        IRuntimesBuilder result = new RuntimesBuilder(Optional.ofNullable(contextBuilder));
        log.atTrace().log("Exiting builder() with IInjectionContextBuilder parameter");
        return result;
    }

    public static IRuntimesBuilder builder(Optional<IInjectionContextBuilder> contextBuilder) {
        log.atTrace().log("Entering builder() with Optional<IInjectionContextBuilder> parameter");
        IRuntimesBuilder result = new RuntimesBuilder(contextBuilder);
        log.atTrace().log("Exiting builder() with Optional<IInjectionContextBuilder> parameter");
        return result;
    }

    @Override
    public IRuntimesBuilder context(IInjectionContextBuilder context) {
        log.atTrace().log("Entering context() method");

        Objects.requireNonNull(context, "Context builder cannot be null");
        this.contextBuilder = context;
        this.provide(context);

        context.withPackage("com.garganttua.core.runtime.annotations");
        context.childContextFactory(new RuntimeContextFactory());
        context.resolvers().withResolver(Input.class, new InputElementResolver());
        context.resolvers().withResolver(Variable.class, new VariableElementResolver());
        context.resolvers().withResolver(Context.class, new ContextElementResolver());
        context.resolvers().withResolver(Exception.class, new ExceptionElementResolver());
        context.resolvers().withResolver(Code.class, new CodeElementResolver());
        context.resolvers().withResolver(ExceptionMessage.class, new ExceptionMessageElementResolver());

        log.atInfo().log("Context builder configured with resolvers");

        log.atTrace().log("Exiting context() method");
        return this;
    }

}
