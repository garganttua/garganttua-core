package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.runtime.IRuntimeStepOnException;
import com.garganttua.core.runtime.RuntimeStepOnException;
import com.garganttua.core.runtime.annotations.OnException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractAutomaticLinkedBuilder<IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepOnException>
        implements IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private Class<? extends Throwable> exception;
    private String stageName = null;
    private String stepName = null;
    private OnException onExceptionForAutoDetection;
    private String runtimeName;

    protected RuntimeStepOnExceptionBuilder(
            IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link,
            String runtimeName,
            Class<? extends Throwable> exception) {
        super(link);
        log.atTrace().log("Entering RuntimeStepOnExceptionBuilder constructor with runtimeName={}, exception={}",
                runtimeName, exception);
        this.exception = Objects.requireNonNull(exception, "Exception cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        log.atInfo().log("RuntimeStepOnExceptionBuilder constructed successfully for exception {}",
                exception.getSimpleName());
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
            String runtimeName,
            Class<? extends Throwable> exception, OnException onException) {
        this(link, runtimeName, exception);
        log.atTrace().log(
                "Entering secondary RuntimeStepOnExceptionBuilder constructor with runtimeName={}, exception={}, onException={}",
                runtimeName, exception, onException);
        this.onExceptionForAutoDetection = Objects.requireNonNull(onException, "OnException annotation cannot be null");
        log.atInfo().log("OnException annotation set for auto-detection");
    }

    @Override
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStage(
            String stageName) {
        log.atTrace().log("Entering fromStage method with stageName={}", stageName);
        this.stageName = Objects.requireNonNull(stageName, "stageName cannot be null");
        log.atInfo().log("Stage name set to '{}'", stageName);
        return this;
    }

    @Override
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStep(
            String stepName) {
        log.atTrace().log("Entering fromStep method with stepName={}", stepName);
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        log.atInfo().log("Step name set to '{}'", stepName);
        return this;
    }

    @Override
    protected IRuntimeStepOnException doBuild() throws DslException {
        log.atTrace().log("Entering doBuild method");
        RuntimeStepOnException result = new RuntimeStepOnException(exception, this.runtimeName, this.stageName,
                this.stepName);
        log.atInfo().log("RuntimeStepOnException built successfully for exception {}", exception.getSimpleName());
        return result;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection method");
        Objects.requireNonNull(onExceptionForAutoDetection, "onExceptionForAutoDetection cannot be null");

        if (onExceptionForAutoDetection.fromStage() != null && !onExceptionForAutoDetection.fromStage().isEmpty()) {
            this.stageName = onExceptionForAutoDetection.fromStage();
            log.atDebug().log("Auto-detected stageName: {}", this.stageName);
        }
        if (onExceptionForAutoDetection.fromStep() != null && !onExceptionForAutoDetection.fromStep().isEmpty()) {
            this.stepName = onExceptionForAutoDetection.fromStep();
            log.atDebug().log("Auto-detected stepName: {}", this.stepName);
        }
        log.atInfo().log("Auto-detection completed for exception {}", exception.getSimpleName());
    }
}