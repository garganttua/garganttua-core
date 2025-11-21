package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.execution.ExecutorChain;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.supplying.IObjectSupplier;

public class Runtime<InputType, OutputType> implements IRuntime<InputType, OutputType> {

    private final String name;
    private final IDiContext diContext;
    private final Class<InputType> inputType;
    private final Class<OutputType> outputType;
    private final Map<String, IRuntimeStage<InputType, OutputType>> stages;
    private final Map<String, IObjectSupplier<?>> presetVariables;

    public Runtime(String name, Map<String, IRuntimeStage<InputType, OutputType>> stages, IDiContext diContext,
            Class<InputType> inputType,
            Class<OutputType> outputType, Map<String, IObjectSupplier<?>> variables) {
        this.stages = Collections
                .synchronizedMap(Map.copyOf(Objects.requireNonNull(stages, "Name cannot be null")));
        this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.diContext = Objects.requireNonNull(diContext, "Context cannot be null");
        this.presetVariables = Collections
                .synchronizedMap(Map.copyOf(Objects.requireNonNull(variables, "Preset variables map cannot be null")));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<IRuntimeResult<InputType, OutputType>> execute(InputType input) throws RuntimeException {
        IRuntimeContext<InputType, OutputType> runtimeContext = null;
        IRuntimeResult<InputType, OutputType> result = null;
        try {
            runtimeContext = this.diContext
                    .newChildContext(IRuntimeContext.class, input, this.outputType, this.presetVariables);

            runtimeContext.onInit().onStart();

            IExecutorChain<IRuntimeContext<InputType, OutputType>> chain = new ExecutorChain<>(false);

            this.stages.values().stream().forEach(
                    stage -> {
                        stage.getSteps().values().stream().forEach(step -> {
                            step.defineExecutionStep(chain);
                        });
                    });

            chain.execute(runtimeContext);
        } catch (Exception e) {
            throw new RuntimeException(e, Optional.ofNullable(runtimeContext));
        } finally {
            if( runtimeContext != null ){
                runtimeContext.onStop();
                result = runtimeContext.getResult();
                runtimeContext.onFlush();
            }
        }

        return Optional.ofNullable(result);
    }
}
