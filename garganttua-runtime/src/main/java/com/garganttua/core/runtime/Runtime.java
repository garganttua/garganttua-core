package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.execution.ExecutorChain;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.supplying.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runtime<InputType, OutputType> implements IRuntime<InputType, OutputType> {

    private final String name;
    private final IDiContext diContext;
    private final Class<InputType> inputType;
    private final Class<OutputType> outputType;
    private final Map<String, IRuntimeStage<InputType, OutputType>> stages;
    private final Map<String, IObjectSupplier<?>> presetVariables;

    public Runtime(
            String name,
            Map<String, IRuntimeStage<InputType, OutputType>> stages,
            IDiContext diContext,
            Class<InputType> inputType,
            Class<OutputType> outputType,
            Map<String, IObjectSupplier<?>> variables) {

        log.atTrace().log(
                "[Runtime.<init>] Initializing Runtime with name={}, inputType={}, outputType={}, stages={}, presetVariables={}",
                name, inputType, outputType, stages, variables);

        this.stages = Collections.synchronizedMap(
                Map.copyOf(Objects.requireNonNull(stages, "Stages map cannot be null")));

        this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.diContext = Objects.requireNonNull(diContext, "Context cannot be null");
        this.presetVariables = Collections.synchronizedMap(
                Map.copyOf(Objects.requireNonNull(variables, "Preset variables map cannot be null")));

        log.atInfo().log("[Runtime.<init>] Runtime initialized successfully with name={}", this.name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<IRuntimeResult<InputType, OutputType>> execute(InputType input) throws RuntimeException {

        log.atInfo()
                .log("Starting runtime execution");

        log.atTrace()
                .log("Runtime input received");

        IRuntimeContext<InputType, OutputType> runtimeContext = null;
        IRuntimeResult<InputType, OutputType> result = null;

        try {

            // CREATE CONTEXT
            log.atDebug()
                    .log("Creating runtime context");

            runtimeContext = this.diContext
                    .newChildContext(IRuntimeContext.class, input, this.outputType, this.presetVariables);

            runtimeContext.onInit().onStart();

            // BUILD EXECUTION CHAIN
            log.atDebug()
                    .log("Building executor chain");

            IExecutorChain<IRuntimeContext<InputType, OutputType>> chain = new ExecutorChain<>(false);

            this.stages.values().forEach(stage -> {
                log.atTrace()
                        .log("Registering stage steps into chain");

                stage.getSteps().values().forEach(step -> {
                    log.atTrace()
                            .log("Registering step");
                    step.defineExecutionStep(chain);
                });
            });

            // EXECUTE
            log.atInfo()
                    .log("Executing runtime chain");

            chain.execute(runtimeContext);

        } catch (Exception e) {

            log.atError()
                    .setCause(e)
                    .log("Fatal error during runtime execution");

            throw new RuntimeException(e, Optional.ofNullable(runtimeContext));

        } finally {

            if (runtimeContext != null) {
                log.atDebug()
                        .log("Stopping runtime context");

                runtimeContext.onStop();

                result = runtimeContext.getResult();

                log.atTrace()
                        .log("Runtime result collected");

                runtimeContext.onFlush();
            }

            log.atInfo()
                    .log("Runtime execution finished");
        }

        return Optional.ofNullable(result);
    }
}