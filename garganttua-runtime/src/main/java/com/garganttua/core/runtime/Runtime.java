package com.garganttua.core.runtime;

import java.util.Map;
import java.util.Objects;

import com.garganttua.core.executor.ExecutorException;
import com.garganttua.core.executor.IExecutorChain;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.executor.chain.ExecutorChain;

public class Runtime<InputType, OutputType> implements IRuntime<InputType, OutputType> {

    private IExecutorChain<IRuntimeContext<InputType, OutputType>> chain = new ExecutorChain<>();
    private String name;
    private IDiContext diContext;
    private Class<InputType> inputType;
    private Class<OutputType> outputType;

    public Runtime(String name, Map<String, IRuntimeStage> stages, IDiContext diContext, Class<InputType> inputType,
            Class<OutputType> outputType) {
        Objects.requireNonNull(stages, "Name cannot be null");
        this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.diContext = Objects.requireNonNull(diContext, "Context cannot be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public IRuntimeResult<InputType, OutputType> execute(InputType input) throws RuntimeException {
        try {
            IRuntimeContext<InputType, OutputType> runtimeContext = this.diContext
                    .newChildContext(IRuntimeContext.class, input, this.outputType);
                    
            this.chain.execute(runtimeContext);
            return runtimeContext.getResult();
        } catch (DiException | ExecutorException e) {
            throw new RuntimeException(e);
        }
    }
}
