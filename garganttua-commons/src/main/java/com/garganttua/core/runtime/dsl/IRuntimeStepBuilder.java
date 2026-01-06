package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.runtime.IRuntimeStep;

/**
 * Builder for configuring a runtime step.
 *
 * <p>
 * IRuntimeStepBuilder provides a fluent DSL for defining individual steps within a runtime stage.
 * A step consists of a main method (operation) and optionally a fallback method for error handling.
 * This builder enables configuration of both.
 * </p>
 *
 * <p>
 * Steps are the atomic units of work in a runtime workflow. They execute methods on the objects
 * provided by the object supplier specified when the step was created.
 * </p>
 *
 * <h2>Usage Example - Simple Step</h2>
 * <pre>{@code
 * builder
 *     .step("validateOrder", () -> new OrderValidator(), Void.class)
 *         .method()
 *             .name("validate")
 *             .parameter(Input.class)
 *             .end()
 *         .end();
 * }</pre>
 *
 * <h2>Usage Example - Step with Fallback</h2>
 * <pre>{@code
 * builder
 *     .step("processOrder", () -> new OrderProcessor(), OrderResult.class)
 *         .method()
 *             .name("process")
 *             .parameter(Input.class)
 *             .output(true)
 *             .katch(IllegalArgumentException.class).code(400).end()
 *             .end()
 *         .fallBack()
 *             .name("handleError")
 *             .parameter(Exception.class)
 *             .output(true)
 *             .end()
 *         .end();
 * }</pre>
 *
 * @param <ExecutionReturn> the return type of the step method
 * @param <StepObjectType> the type of object containing the step method
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see IRuntimeStageBuilder
 * @see IRuntimeStepMethodBuilder
 * @see IRuntimeStepFallbackBuilder
 * @see IRuntimeStep
 */
public interface IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                IAutomaticLinkedBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStageBuilder<InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>>,
                IContextBuilderObserver {

        /**
         * Begins configuration of the main method for this step.
         *
         * <p>
         * The main method is the primary operation executed by the step. It corresponds
         * to {@code @Operation} methods in the annotation-based approach.
         * </p>
         *
         * @return a builder for configuring the method
         * @throws DslException if the method configuration is invalid
         * @see IRuntimeStepMethodBuilder
         * @see com.garganttua.core.runtime.annotations.Operation
         */
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method() throws DslException;

        /**
         * Begins configuration of a fallback method for error handling.
         *
         * <p>
         * The fallback method is invoked when the main method throws an uncaught exception.
         * It corresponds to {@code @FallBack} methods in the annotation-based approach.
         * </p>
         *
         * @return a builder for configuring the fallback method
         * @throws DslException if the fallback configuration is invalid
         * @see IRuntimeStepFallbackBuilder
         * @see com.garganttua.core.runtime.annotations.FallBack
         */
        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallBack() throws DslException;

}
