package com.garganttua.core.runtime.dsl;

import static com.garganttua.core.injection.BeanDefinition.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
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
public class RuntimesBuilder extends AbstractAutomaticBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>
        implements IRuntimesBuilder {

    private Map<String, IRuntimeBuilder<?, ?>> runtimeBuilders = new HashMap<>();
    private IDiContextBuilder contextBuilder;
    private boolean canBuild = false;
    private IDiContext context = null;

    private RuntimesBuilder(Optional<IDiContextBuilder> contextBuilder) {
        Objects.requireNonNull(contextBuilder, "Optional context builder cannot be null");
        this.contextBuilder = contextBuilder.orElse(null);
        contextBuilder.ifPresent(c -> c.observer(this));
        contextBuilder.ifPresent(c -> c.withPackage("com.garganttua.core.runtime.annotations"));
        contextBuilder.ifPresent(c -> c.childContextFactory(new RuntimeContextFactory()));

        log.atInfo()
                .addKeyValue("hasContextBuilder", contextBuilder.isPresent())
                .log("RuntimesBuilder initialized");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String name,
            Class<InputType> inputType,
            Class<OutputType> outputType) {

        log.atTrace()
                .addKeyValue("method", "runtime")
                .addKeyValue("runtimeName", name)
                .log("Entering runtime() method");

        Objects.requireNonNull(this.contextBuilder, "Context builder cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");

        log.atTrace()
                .addKeyValue("inputType", inputType.getSimpleName())
                .addKeyValue("outputType", outputType.getSimpleName())
                .log("Validated runtime input parameters");

        IRuntimeBuilder<InputType, OutputType> runtimeBuilder;
        if (!this.runtimeBuilders.containsKey(name)) {
            runtimeBuilder = new RuntimeBuilder<>(this, name, inputType, outputType);
            this.runtimeBuilders.put(name, runtimeBuilder);
            log.atInfo().addKeyValue("runtimeName", name).log("Created new runtime builder");
        } else {
            runtimeBuilder = (IRuntimeBuilder<InputType, OutputType>) this.runtimeBuilders.get(name);
            log.atDebug().addKeyValue("runtimeName", name).log("Reusing existing runtime builder");
        }

        if (this.context != null) {
            runtimeBuilder.handle(context);
            log.atDebug().addKeyValue("runtimeName", name).log("Runtime builder handled with context");
        }

        log.atTrace()
                .addKeyValue("runtimeName", name)
                .log("Exiting runtime() method");

        return runtimeBuilder;
    }

    @Override
    protected Map<String, IRuntime<?, ?>> doBuild() throws DslException {
        log.atTrace().log("Entering doBuild() method");

        if (!this.canBuild) {
            log.atError().log("Attempt to build before authorization");
            throw new DslException("Build is not yet authorized");
        }

        log.atInfo().addKeyValue("runtimeCount", runtimeBuilders.size()).log("Building all runtimes");

        Map<String, IRuntime<?, ?>> result = this.runtimeBuilders.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                e -> {
                    log.atDebug().addKeyValue("runtimeName", e.getKey()).log("Building individual runtime");
                    return e.getValue().build();
                }));

        log.atTrace().addKeyValue("builtCount", result.size()).log("Exiting doBuild() method");
        return result;
    }

    @Override
    protected void doAutoDetection() {
        log.atTrace().log("Entering doAutoDetection() method");

        if (!this.canBuild) {
            log.atError().log("Attempt to auto-detect runtimes before build authorization");
            throw new DslException("Build is not yet authorized");
        }

        List<?> definitions = this.context
                .queryBeans(example(null, Optional.empty(), Optional.empty(), Set.of(RuntimeDefinition.class)));

        log.atInfo().addKeyValue("foundDefinitions", definitions.size()).log("Auto-detecting runtimes");

        definitions.forEach(this::createAutoDetectedRuntime);

        log.atTrace().log("Exiting doAutoDetection() method");
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
                .addKeyValue("runtimeName", runtimeName)
                .addKeyValue("inputType", input.getSimpleName())
                .addKeyValue("outputType", output.getSimpleName())
                .log("Creating auto-detected runtime builder");

        IRuntimeBuilder<?, ?> runtimeBuilder = new RuntimeBuilder<>(this, runtimeName, input, output,
                runtimeDefinitionObject).autoDetect(true);

        runtimeBuilder.handle(this.context);
        this.runtimeBuilders.put(runtimeName, runtimeBuilder);

        log.atInfo().addKeyValue("runtimeName", runtimeName).log("Auto-detected runtime registered");
    }

    public static IRuntimesBuilder builder() {
        log.atTrace().log("Entering builder() with no parameters");
        IRuntimesBuilder result = new RuntimesBuilder(Optional.empty());
        log.atTrace().log("Exiting builder() with no parameters");
        return result;
    }

    public static IRuntimesBuilder builder(IDiContextBuilder contextBuilder) {
        log.atTrace().log("Entering builder() with IDiContextBuilder parameter");
        IRuntimesBuilder result = new RuntimesBuilder(Optional.ofNullable(contextBuilder));
        log.atTrace().log("Exiting builder() with IDiContextBuilder parameter");
        return result;
    }

    public static IRuntimesBuilder builder(Optional<IDiContextBuilder> contextBuilder) {
        log.atTrace().log("Entering builder() with Optional<IDiContextBuilder> parameter");
        IRuntimesBuilder result = new RuntimesBuilder(contextBuilder);
        log.atTrace().log("Exiting builder() with Optional<IDiContextBuilder> parameter");
        return result;
    }

    @Override
    public IRuntimesBuilder context(IDiContextBuilder context) {
        log.atTrace().log("Entering context() method");

        this.contextBuilder = Objects.requireNonNull(context, "Context builder cannot be null");
        this.contextBuilder.observer(this);
        this.contextBuilder.withPackage("com.garganttua.core.runtime.annotations");
        this.contextBuilder.childContextFactory(new RuntimeContextFactory());
        this.contextBuilder.resolvers().withResolver(Input.class, new InputElementResolver());
        this.contextBuilder.resolvers().withResolver(Variable.class, new VariableElementResolver());
        this.contextBuilder.resolvers().withResolver(Context.class, new ContextElementResolver());
        this.contextBuilder.resolvers().withResolver(Exception.class, new ExceptionElementResolver());
        this.contextBuilder.resolvers().withResolver(Code.class, new CodeElementResolver());
        this.contextBuilder.resolvers().withResolver(ExceptionMessage.class, new ExceptionMessageElementResolver());

        log.atInfo().log("Context builder configured with resolvers");

        log.atTrace().log("Exiting context() method");
        return this;
    }

    @Override
    public void handle(IDiContext context) {
        log.atTrace().log("Entering handle() method");

        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.canBuild = true;

        log.atInfo().addKeyValue("runtimeCount", runtimeBuilders.size())
                .log("Handling all runtime builders with context");

        this.runtimeBuilders.values().forEach(b -> {
            log.atDebug().log("Handling individual runtime builder");
            b.handle(context);
        });

        log.atTrace().log("Exiting handle() method");
    }
}