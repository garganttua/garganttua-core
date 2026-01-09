package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepFallbackBinder;

/**
 * Builder for configuring a step's fallback method for error recovery.
 *
 * <p>
 * IRuntimeStepFallbackBuilder provides a fluent DSL for configuring fallback methods that are
 * invoked when a step's main method throws an uncaught exception. It extends {@link IMethodBinderBuilder}
 * to provide method binding capabilities and adds runtime-specific configuration for output, variables,
 * and exception filtering.
 * </p>
 *
 * <p>
 * This builder is the DSL equivalent of configuring {@code @FallBack} methods with annotations
 * like {@code @Output}, {@code @Variable}, and {@code @OnException}.
 * </p>
 *
 * <h2>Usage Example - Basic Fallback</h2>
 * <pre>{@code
 * stepBuilder
 *     .fallBack()
 *         .name("handleError")
 *         .parameter(Exception.class)  // @Exception
 *         .parameter(Input.class)      // @Input
 *         .output(true)                // @Output
 *         .end();
 * }</pre>
 *
 * <h2>Usage Example - Conditional Fallback</h2>
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
 *         .nullable(false)
 *         .end();
 * }</pre>
 *
 * @param <ExecutionReturn> the return type of the fallback method
 * @param <StepObjectType> the type of object containing the fallback method
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see IRuntimeStepBuilder
 * @see IRuntimeStepOnExceptionBuilder
 * @see com.garganttua.core.runtime.annotations.FallBack
 */
public interface IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                IMethodBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>,
                IContextBuilderObserver {

        /**
         * Configures the fallback method's return value to be stored as a variable.
         *
         * <p>
         * Equivalent to {@code @Variable(name = "variableName")} on a fallback method.
         * </p>
         *
         * @param variableName the name for storing the return value
         * @return this builder for method chaining
         * @see com.garganttua.core.runtime.annotations.Variable
         */
        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(
                        String variableName);

        /**
         * Configures whether the fallback method's return value should be the runtime output.
         *
         * <p>
         * Equivalent to {@code @Output} annotation on a fallback method.
         * </p>
         *
         * @param output true to set return value as output, false otherwise
         * @return this builder for method chaining
         * @see com.garganttua.core.runtime.annotations.Output
         */
        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output);

        /**
         * Begins configuration of exception filtering criteria.
         *
         * <p>
         * This fallback will only be invoked for exceptions matching the specified criteria.
         * Equivalent to {@code @OnException(exception = ...)} annotation.
         * </p>
         *
         * @param exception the exception class to handle
         * @return a builder for configuring the exception criteria
         * @throws DslException if the configuration is invalid
         * @see IRuntimeStepOnExceptionBuilder
         * @see com.garganttua.core.runtime.annotations.OnException
         */
        IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException(
                        Class<? extends Throwable> exception) throws DslException;

        /**
         * Configures whether null return values are acceptable.
         *
         * @param nullable true to allow null returns, false otherwise
         * @return this builder for method chaining
         */
        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> nullable(
                        boolean nullable);

}
