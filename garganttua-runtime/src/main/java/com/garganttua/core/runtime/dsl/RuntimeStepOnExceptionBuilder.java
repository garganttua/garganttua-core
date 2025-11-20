package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.runtime.IRuntimeStepOnException;
import com.garganttua.core.runtime.RuntimeStepOnException;
import com.garganttua.core.runtime.annotations.OnException;

public class RuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractAutomaticLinkedBuilder<IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepOnException>
        implements IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private Class<? extends Throwable> exception;
    private String stageName = null;
    private String stepName = null;
    private OnException onExceptionForAutoDetection;

    protected RuntimeStepOnExceptionBuilder(
            IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link,
            Class<? extends Throwable> exception) {
        super(link);
        this.exception = Objects.requireNonNull(exception, "Exception cannot be null");
    }

    /**
     * Secondary ctor used only for auto detection
     * 
     * @param link
     * @param exception
     * @param onException
     */
    protected RuntimeStepOnExceptionBuilder(
            IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link,
            Class<? extends Throwable> exception, OnException onException) {
        this(link, exception);
        this.onExceptionForAutoDetection = Objects.requireNonNull(onException, "OnException annotation cannot be null");
    }

    @Override
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStage(
            String stageName) {
        this.stageName = Objects.requireNonNull(stageName, "stageName cannot be null");
        return this;
    }

    @Override
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStep(
            String stepName) {
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        return this;
    }

    @Override
    protected IRuntimeStepOnException doBuild() throws DslException {
        return new RuntimeStepOnException(exception, this.stageName, this.stepName);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        Objects.requireNonNull(onExceptionForAutoDetection, "onExceptionForAutoDetection cannot be null");
        if (onExceptionForAutoDetection.fromStage() != null && !onExceptionForAutoDetection.fromStage().isEmpty())
            this.stageName = onExceptionForAutoDetection.fromStage();
        if (onExceptionForAutoDetection.fromStep() != null && !onExceptionForAutoDetection.fromStep().isEmpty())
            this.stepName = onExceptionForAutoDetection.fromStep();
    }

}
