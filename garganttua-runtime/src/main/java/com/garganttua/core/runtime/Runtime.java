package com.garganttua.core.runtime;

import java.util.Map;
import java.util.Objects;

import com.garganttua.core.executor.IExecutorChain;


public class Runtime<InputType, OutputType> implements IRuntime<InputType, OutputType> {

    private IExecutorChain<IRuntimeResult<InputType, OutputType>> chain;
    private String name;

    public Runtime(String name, Map<String, IRuntimeStage> stages) {
        Objects.requireNonNull(stages, "Name cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
    }


    @Override
    public IRuntimeResult<InputType, OutputType> execute(InputType input) throws RuntimeException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }}
