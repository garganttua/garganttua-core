package com.garganttua.core.runtime.dsl;

import static com.garganttua.core.injection.BeanDefinition.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.RuntimeContextFactory;
import com.garganttua.core.runtime.annotations.RuntimeDefinition;

public class RuntimesBuilder extends AbstractAutomaticBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>
        implements IRuntimesBuilder {

    private Map<String, IRuntimeBuilder<?, ?>> runtimeBuilders = new HashMap<>();
    private IDiContextBuilder contextBuilder;
    private boolean canBuild = false;
    private IDiContext context = null;
    private String[] packages;

    private RuntimesBuilder(Optional<IDiContextBuilder> contextBuilder) {
        Objects.requireNonNull(contextBuilder, "Optional context builder cannot be null");
        this.contextBuilder = contextBuilder.orElse(null);
        contextBuilder.ifPresent(c -> c.observer(this));
        contextBuilder.ifPresent(c -> c.childContextFactory(new RuntimeContextFactory()));
        contextBuilder.ifPresent(c -> this.packages = c.getPackages());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String name,
            Class<InputType> inputType,
            Class<OutputType> outputType) {
        Objects.requireNonNull(this.contextBuilder, "Context builder cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        IRuntimeBuilder<InputType, OutputType> runtimeBuilder;
        if (!this.runtimeBuilders.containsKey(name)) {
            runtimeBuilder = new RuntimeBuilder<>(this, name, inputType, outputType);
            this.runtimeBuilders.put(name, runtimeBuilder);
        } else {
            runtimeBuilder = (IRuntimeBuilder<InputType, OutputType>) this.runtimeBuilders.get(name);
        }
        if (this.context != null) {
            runtimeBuilder.handle(context);
        }
        return runtimeBuilder;
    }

    @Override
    protected Map<String, IRuntime<?, ?>> doBuild() throws DslException {
        if (!this.canBuild)
            throw new DslException("Build is not yet authorized");

        return this.runtimeBuilders.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().toLowerCase(),
                e -> e.getValue().build()));
    }

    @Override
    protected void doAutoDetection() {
        List<?> definitions = this.context
                .queryBeans(example(null, Optional.empty(), Optional.empty(), Set.of(RuntimeDefinition.class)));
        definitions.stream().forEach(this::createAutoDetectedRuntime);
    }

    private void createAutoDetectedRuntime(Object runtimeDefinitionObject) {
        String runtimeName = UUID.randomUUID().toString();
        RuntimeDefinition runtimeDefinition = runtimeDefinitionObject.getClass().getAnnotation(RuntimeDefinition.class);
        Class<?> input = runtimeDefinition.input();
        Class<?> output = runtimeDefinition.output();
        Named runtimeNameAnnotation = runtimeDefinitionObject.getClass().getAnnotation(Named.class);
        if (runtimeNameAnnotation != null) {
            runtimeName = runtimeNameAnnotation.value();
        }
        IRuntimeBuilder<?,?> runtimeBuilder = new RuntimeBuilder<>(this, runtimeName, input, output, runtimeDefinitionObject).autoDetect(true);
        runtimeBuilder.handle(this.context);
        this.runtimeBuilders.put(runtimeName, runtimeBuilder);
    }

    public static IRuntimesBuilder builder() {
        return new RuntimesBuilder(Optional.empty());
    }

    public static IRuntimesBuilder builder(IDiContextBuilder contextBuilder) {
        return new RuntimesBuilder(Optional.ofNullable(contextBuilder));
    }

    public static IRuntimesBuilder builder(Optional<IDiContextBuilder> contextBuilder) {
        return new RuntimesBuilder(contextBuilder);
    }

    @Override
    public IRuntimesBuilder context(IDiContextBuilder context) {
        this.contextBuilder = Objects.requireNonNull(context, "Context builder cannot be null");
        this.contextBuilder.observer(this);
        this.contextBuilder.childContextFactory(new RuntimeContextFactory());
        this.packages = this.contextBuilder.getPackages();
        return this;
    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.canBuild = true;
        this.runtimeBuilders.values().forEach(b -> b.handle(context));
    }

}
