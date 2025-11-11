package com.garganttua.core.runtime.dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.RuntimeContextFactory;

public class RuntimesBuilder extends AbstractAutomaticBuilder<IRuntimesBuilder, Map<String, IRuntime<?,?>>>
        implements IRuntimesBuilder {

    private Map<String, IRuntimeBuilder<?,?>> runtimeBuilders = new HashMap<>();
    private IDiContextBuilder contextBuilder;
    private boolean canBuild = false;
    private IDiContext context = null;

    private RuntimesBuilder(Optional<IDiContextBuilder> contextBuilder) {
        Objects.requireNonNull(contextBuilder, "Optional context builder cannot be null");
        this.contextBuilder = contextBuilder.orElse(null);
        if (this.contextBuilder != null) {
            this.contextBuilder.observer(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String name, Class<InputType> inputType,
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
        if( this.context != null ){
            runtimeBuilder.handle(context);
        }
        return runtimeBuilder;
    }

    @Override
    protected Map<String, IRuntime<?,?>> doBuild() throws DslException {
        if (!this.canBuild)
            throw new DslException("Build is not yet authorized");

        return this.runtimeBuilders.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().toLowerCase(),
                e -> {
                    try {
                        return e.getValue().build();
                    } catch (DslException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                        return null;
                    }
                }));

    }

    @Override
    protected void doAutoDetection() {
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
        return this;
    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.canBuild = true;
        this.runtimeBuilders.values().forEach(b -> b.handle(context));
    }

}
