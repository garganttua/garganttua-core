package com.garganttua.core.runtime.dsl;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeStepOnException;
import com.garganttua.core.runtime.RuntimeStepOnException;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder for a fallback's {@code @OnException} handler, declaring which
 * exception (optionally originating from a specific step) the fallback responds to.
 *
 * @param <ExecutionReturn> the operation method return type
 * @param <StepObjectType>  the type of the object holding the step methods
 * @param <InputType>       the runtime input type
 * @param <OutputType>      the runtime output type
 */
@Reflected
public class RuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractAutomaticLinkedBuilder<IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepOnException>
        implements IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStepOnExceptionBuilder.class);

    private IClass<? extends Throwable> exception;
    private String stepName = null;
    private OnException onExceptionForAutoDetection;
    private String runtimeName;

    /**
     * Creates an on-exception handler builder.
     *
     * @param link        the parent fallback builder
     * @param runtimeName the owning runtime name
     * @param exception   the exception type this handler responds to
     */
    protected RuntimeStepOnExceptionBuilder(
            IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> link,
            String runtimeName,
            Class<? extends Throwable> exception) {
        super(link);
        log.trace("Entering RuntimeStepOnExceptionBuilder constructor with runtimeName={}, exception={}",
                runtimeName, exception);
        this.exception = IClass.getClass(Objects.requireNonNull(exception, "Exception cannot be null"));
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        log.debug("RuntimeStepOnExceptionBuilder constructed successfully for exception {}",
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
        log.trace(
                "Entering secondary RuntimeStepOnExceptionBuilder constructor with runtimeName={}, exception={}, onException={}",
                runtimeName, exception, onException);
        this.onExceptionForAutoDetection = Objects.requireNonNull(onException, "OnException annotation cannot be null");
        log.debug("OnException annotation set for auto-detection");
    }

    /**
     * Restricts this handler to exceptions originating from the named step.
     *
     * @param stepName the source step name
     * @return this builder for chaining
     */
    @Override
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStep(
            String stepName) {
        log.trace("Entering fromStep method with stepName={}", stepName);
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        log.debug("Step name set to '{}'", stepName);
        return this;
    }

    @Override
    protected IRuntimeStepOnException doBuild() throws DslException {
        log.trace("Entering doBuild method");
        RuntimeStepOnException result = new RuntimeStepOnException(exception, this.runtimeName,
                this.stepName);
        log.debug("RuntimeStepOnException built successfully for exception {}", exception.getSimpleName());
        return result;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("Entering doAutoDetection method");
        Objects.requireNonNull(onExceptionForAutoDetection, "onExceptionForAutoDetection cannot be null");

        if (onExceptionForAutoDetection.fromStep() != null && !onExceptionForAutoDetection.fromStep().isEmpty()) {
            this.stepName = onExceptionForAutoDetection.fromStep();
            log.debug("Auto-detected stepName: {}", this.stepName);
        }
        log.debug("Auto-detection completed for exception {}", exception.getSimpleName());
    }
}
