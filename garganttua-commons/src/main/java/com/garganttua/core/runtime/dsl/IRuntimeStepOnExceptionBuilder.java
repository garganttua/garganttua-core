package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStepOnException;

/**
 * Builder for configuring exception filtering criteria in fallback methods.
 *
 * <p>
 * IRuntimeStepOnExceptionBuilder provides a fluent DSL for specifying which exceptions should
 * trigger a fallback method based on the source stage and step. This enables cross-step exception
 * handling where a fallback in one step can respond to exceptions from other steps.
 * </p>
 *
 * <p>
 * This builder is the DSL equivalent of the {@code @OnException} annotation and is used in
 * combination with {@link IRuntimeStepFallbackBuilder} to configure conditional fallback behavior.
 * </p>
 *
 * <h2>Usage Example - Handle Exception from Specific Step</h2>
 * <pre>{@code
 * stepBuilder
 *     .fallBack()
 *         .name("handleValidationError")
 *         .parameter(Exception.class)
 *         .onException(IllegalArgumentException.class)
 *             .fromStage("validation")
 *             .fromStep("validateAmount")
 *             .end()
 *         .output(true)
 *         .end();
 * }</pre>
 *
 * <h2>Usage Example - Handle All Exceptions from Stage</h2>
 * <pre>{@code
 * stepBuilder
 *     .fallBack()
 *         .name("handleDatabaseErrors")
 *         .parameter(Exception.class)
 *         .onException(Exception.class)
 *             .fromStage("database")  // Handle any exception from database stage
 *             .end()
 *         .output(true)
 *         .end();
 * }</pre>
 *
 * @param <ExecutionReturn> the return type of the fallback method
 * @param <StepObjectType> the type of object containing the fallback method
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see IRuntimeStepFallbackBuilder
 * @see IRuntimeStepOnException
 * @see com.garganttua.core.runtime.annotations.OnException
 */
public interface IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends
        IAutomaticLinkedBuilder<IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepOnException> {

    /**
     * Specifies the source stage name to filter exceptions by.
     *
     * <p>
     * The fallback will only be triggered for exceptions from steps in the stage
     * with this name. If not specified, exceptions from any stage will match.
     * </p>
     *
     * @param stageName the stage name to match
     * @return this builder for method chaining
     * @see com.garganttua.core.runtime.annotations.OnException#fromStage()
     */
    IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStage(String stageName);

    /**
     * Specifies the source step name to filter exceptions by.
     *
     * <p>
     * The fallback will only be triggered for exceptions from the step with this name.
     * If not specified, exceptions from any step will match.
     * </p>
     *
     * @param stepName the step name to match
     * @return this builder for method chaining
     * @see com.garganttua.core.runtime.annotations.OnException#fromStep()
     */
    IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStep(String stepName);
}
