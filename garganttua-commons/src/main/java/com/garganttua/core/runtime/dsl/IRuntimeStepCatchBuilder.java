package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStepCatch;

/**
 * Builder for configuring exception catching behavior in a runtime step.
 *
 * <p>
 * IRuntimeStepCatchBuilder provides a fluent DSL for configuring how specific exception types
 * should be caught and handled within a step. It allows setting a result code to be applied
 * when the configured exception is caught.
 * </p>
 *
 * <p>
 * This builder is the DSL equivalent of the {@code @Catch} annotation and is typically used
 * in combination with {@link IRuntimeStepMethodBuilder} to configure exception handling for
 * step methods.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * stepBuilder
 *     .method()
 *         .name("processOrder")
 *         .parameter(Input.class)
 *         .output(true)
 *         .katch(IllegalArgumentException.class)
 *             .code(400)  // Set error code 400 for IllegalArgumentException
 *             .end()
 *         .katch(IOException.class)
 *             .code(500)  // Set error code 500 for IOException
 *             .end()
 *         .end();
 * }</pre>
 *
 * @param <ExecutionReturn> the return type of the step method
 * @param <StepObjectType> the type of object containing the step method
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see IRuntimeStepMethodBuilder
 * @see IRuntimeStepCatch
 * @see com.garganttua.core.runtime.annotations.Catch
 */
public interface IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends IAutomaticLinkedBuilder<IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepCatch> {

    /**
     * Sets the result code to apply when this exception is caught.
     *
     * <p>
     * The code will be set in the runtime context and included in the final result.
     * By convention, non-zero codes indicate errors.
     * </p>
     *
     * @param i the error code to set
     * @return this builder for method chaining
     * @see com.garganttua.core.runtime.IRuntimeContext#setCode(int)
     * @see com.garganttua.core.runtime.annotations.Catch#code()
     */
    IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(int i);

}
