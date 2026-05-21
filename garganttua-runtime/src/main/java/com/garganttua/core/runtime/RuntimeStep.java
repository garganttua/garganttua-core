package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.execution.IExecutor;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.execution.IFallBackExecutor;
import com.garganttua.core.observability.ObservabilityEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStep<ExecutionReturn, InputType, OutputType>
        implements IRuntimeStep<ExecutionReturn, InputType, OutputType> {

    private final String stepName;
    private Class<ExecutionReturn> executionReturn;
    private IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> operationBinder;
    private Optional<IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>> fallbackBinder;
    private String runtimeName;

    public RuntimeStep(String runtimeName, String stepName, Class<ExecutionReturn> executionReturn,
            IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> operationBinder,
            Optional<IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>> fallbackBinder) {

        log.atTrace().log("[RuntimeStep.<init>] Initializing RuntimeStep: runtime={}, step={}, executionReturn={}, hasFallback={}",
                runtimeName, stepName, executionReturn, fallbackBinder.isPresent());

        this.runtimeName = runtimeName;
        this.stepName = stepName;
        this.executionReturn = executionReturn;
        this.operationBinder = operationBinder;
        this.fallbackBinder = fallbackBinder;

        log.atDebug().log("{}Initialized RuntimeStep with executionReturn={}, fallbackPresent={}",
                logLineHeader(), executionReturn, fallbackBinder.isPresent());
    }

    @Override
    public String getStepName() {
        log.atTrace().log("{}Returning step name: {}", logLineHeader(), stepName);
        return stepName;
    }

    @Override
    public void defineExecutionStep(IExecutorChain<IRuntimeContext<InputType, OutputType>> chain) {
        log.atDebug().log("{}Defining execution step in chain. Fallback present: {}", logLineHeader(), fallbackBinder.isPresent());

        String source = "runtime:" + runtimeName + ":step:" + stepName;

        IExecutor<IRuntimeContext<InputType, OutputType>> observedExecutor = (ctx, next) -> {
            try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.joinCurrent()) {
                scope.fireStart(source);
                try {
                    operationBinder.execute(ctx, next);
                    scope.fireEnd(source);
                } catch (RuntimeException | com.garganttua.core.execution.ExecutorException e) {
                    scope.fireError(source, e);
                    throw e;
                }
            }
        };

        if (this.fallbackBinder.isPresent()) {
            log.atDebug().log("{}Adding executor with fallback", logLineHeader());
            String fbSource = source + ":fallback";
            IFallBackExecutor<IRuntimeContext<InputType, OutputType>> observedFallback = (ctx, next) -> {
                try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.joinCurrent()) {
                    scope.fireStart(fbSource);
                    try {
                        fallbackBinder.get().fallBack(ctx, next);
                        scope.fireEnd(fbSource);
                    } catch (RuntimeException e) {
                        scope.fireError(fbSource, e);
                        throw e;
                    }
                }
            };
            chain.addExecutor(observedExecutor, observedFallback);
        } else {
            log.atDebug().log("{}Adding executor without fallback", logLineHeader());
            chain.addExecutor(observedExecutor);
        }
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "] ";
    }

}
