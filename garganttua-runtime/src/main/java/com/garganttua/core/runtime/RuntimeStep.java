package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.execution.IExecutor;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.execution.IFallBackExecutor;
import com.garganttua.core.observability.ObservabilityEmitter;

/**
 * Default {@link IRuntimeStep} implementation: registers an observed operation
 * executor (and optional fallback executor) into an {@link IExecutorChain}.
 *
 * <p>Each step emits start/end/error observability events under
 * {@code runtime:<runtime>:step:<step>} (and {@code :fallback} for its fallback).</p>
 *
 * @param <ExecutionReturn> the step's execution return type
 * @param <InputType>       the runtime input type
 * @param <OutputType>      the runtime output type
 */
public class RuntimeStep<ExecutionReturn, InputType, OutputType>
        implements IRuntimeStep<ExecutionReturn, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStep.class);

    private final String stepName;
    private Class<ExecutionReturn> executionReturn;
    private IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> operationBinder;
    private Optional<IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>> fallbackBinder;
    private String runtimeName;

    /**
     * Creates a runtime step.
     *
     * @param runtimeName     the owning runtime's name (for observability and logging)
     * @param stepName        this step's name
     * @param executionReturn the step's declared return type
     * @param operationBinder the binder that performs the step's operation
     * @param fallbackBinder  an optional fallback binder invoked on failure
     */
    public RuntimeStep(String runtimeName, String stepName, Class<ExecutionReturn> executionReturn,
            IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> operationBinder,
            Optional<IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>> fallbackBinder) {

        log.trace("[RuntimeStep.<init>] Initializing RuntimeStep: runtime={}, step={}, executionReturn={}, hasFallback={}",
                runtimeName, stepName, executionReturn, fallbackBinder.isPresent());

        this.runtimeName = runtimeName;
        this.stepName = stepName;
        this.executionReturn = executionReturn;
        this.operationBinder = operationBinder;
        this.fallbackBinder = fallbackBinder;

        log.debug("{}Initialized RuntimeStep with executionReturn={}, fallbackPresent={}",
                logLineHeader(), executionReturn, fallbackBinder.isPresent());
    }

    @Override
    public String getStepName() {
        log.trace("{}Returning step name: {}", logLineHeader(), stepName);
        return stepName;
    }

    @Override
    public void defineExecutionStep(IExecutorChain<IRuntimeContext<InputType, OutputType>> chain) {
        log.debug("{}Defining execution step in chain. Fallback present: {}", logLineHeader(), fallbackBinder.isPresent());

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
            log.debug("{}Adding executor with fallback", logLineHeader());
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
            log.debug("{}Adding executor without fallback", logLineHeader());
            chain.addExecutor(observedExecutor);
        }
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "] ";
    }

}
