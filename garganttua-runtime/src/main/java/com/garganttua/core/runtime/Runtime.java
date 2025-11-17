package com.garganttua.core.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.execution.ExecutorChain;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.supplying.IObjectSupplier;

public class Runtime<InputType, OutputType> implements IRuntime<InputType, OutputType> {

    
    private String name;
    private IDiContext diContext;
    private Class<InputType> inputType;
    private Class<OutputType> outputType;
    private Map<String, IRuntimeStage> stages;
    private Map<String, IObjectSupplier<?>> presetVariables = new HashMap<>();

    public Runtime(String name, Map<String, IRuntimeStage> stages, IDiContext diContext, Class<InputType> inputType,
            Class<OutputType> outputType, Map<String, IObjectSupplier<?>> variables) {
        this.stages = Objects.requireNonNull(stages, "Name cannot be null");
        this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.diContext = Objects.requireNonNull(diContext, "Context cannot be null");
        this.presetVariables.putAll(Objects.requireNonNull(variables, "Preset variables map cannot be null"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public IRuntimeResult<InputType, OutputType> execute(InputType input) throws RuntimeException {
        try {

            IExecutorChain<IRuntimeContext<InputType, OutputType>> chain = new ExecutorChain<>();

            this.stages.values().stream().forEach(
                stage -> {
                    stage.getSteps().values().stream().forEach(step -> {
                        //step.
                    });
                }
            );
           // chain.













            IRuntimeContext<InputType, OutputType> runtimeContext = this.diContext
                    .newChildContext(IRuntimeContext.class, input, this.outputType, this.presetVariables);
                    
            chain.execute(runtimeContext);
            return runtimeContext.getResult();
        } catch (DiException | ExecutorException e) {
            throw new RuntimeException(e);
        }
    }
}
