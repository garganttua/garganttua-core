package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

/**
 * Builder for configuring a runtime stage.
 *
 * <p>
 * IRuntimeStageBuilder provides a fluent DSL for defining stages within a runtime workflow.
 * A stage is a logical grouping of steps that execute sequentially. The builder allows adding
 * steps with precise ordering control.
 * </p>
 *
 * <p>
 * Each step requires an object supplier (lambda or factory) that provides the object containing
 * the method to execute, the expected return type, and additional configuration through the
 * returned {@link IRuntimeStepBuilder}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * builder
 *     .stage("validation")
 *         .step("validateAmount", () -> new AmountValidator(), Void.class)
 *             .method().name("validate").parameter(Input.class).end()
 *             .end()
 *         .step("validateCustomer", () -> new CustomerValidator(), Boolean.class)
 *             .method().name("validate").parameter(Input.class).end()
 *             .end()
 *         .end()
 *     .stage("processing")
 *         .step("process", () -> new OrderProcessor(), OrderResult.class)
 *             .method().name("process").parameter(Input.class).output(true).end()
 *             .end()
 *         .end();
 * }</pre>
 *
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see IRuntimeBuilder
 * @see IRuntimeStepBuilder
 * @see IRuntimeStage
 */
public interface IRuntimeStageBuilder<InputType, OutputType> extends IAutomaticLinkedBuilder<IRuntimeStageBuilder<InputType, OutputType>, IRuntimeBuilder<InputType, OutputType>, IRuntimeStage<InputType, OutputType> >, IContextBuilderObserver {

    /**
     * Creates a new step at the end of this stage.
     *
     * <p>
     * Steps are executed sequentially in the order they are added.
     * </p>
     *
     * @param <StepObjectType> the type of object containing the step method
     * @param <ExecutionReturn> the return type of the step method
     * @param string the step name
     * @param objectSupplier supplier that provides the object containing the method to execute
     * @param returnType the class representing the method's return type
     * @return a builder for configuring the step
     * @see IRuntimeStepBuilder
     */
    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

    /**
     * Creates a new step at a specific position relative to existing steps.
     *
     * <p>
     * This allows inserting steps before or after existing steps by name,
     * enabling precise control over execution order within the stage.
     * </p>
     *
     * @param <StepObjectType> the type of object containing the step method
     * @param <ExecutionReturn> the return type of the step method
     * @param string the step name
     * @param position the position where to insert the step
     * @param objectSupplier supplier that provides the object containing the method to execute
     * @param returnType the class representing the method's return type
     * @return a builder for configuring the step
     * @see com.garganttua.core.runtime.RuntimeStepPosition
     */
    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, OrderedMapPosition<String> position, IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

}
