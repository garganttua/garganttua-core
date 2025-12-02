package com.garganttua.core.runtime;

import java.util.Map;

/**
 * Represents a logical grouping of related processing steps within a runtime workflow.
 *
 * <p>
 * A runtime stage is a collection of steps that are executed sequentially as part of the overall
 * workflow. Stages provide a way to organize complex workflows into manageable sections, each
 * responsible for a specific phase of processing (e.g., validation, transformation, persistence).
 * </p>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Sequential Execution</b> - Steps within a stage execute in the order they are defined</li>
 *   <li><b>Shared Context</b> - All steps within a stage share the same runtime context</li>
 *   <li><b>Named Steps</b> - Each step has a unique name within the stage for identification</li>
 *   <li><b>Exception Handling</b> - Exceptions can be caught and handled at the step or stage level</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Programmatic DSL approach
 * IRuntimeBuilder<Order, OrderResult> builder = runtimesBuilder
 *     .runtime("orderProcessing", Order.class, OrderResult.class)
 *         .stage("validation")  // First stage
 *             .step("checkStock", () -> new StockChecker(), Boolean.class)
 *                 .method().name("check").parameter(Input.class).end()
 *                 .end()
 *             .step("validatePayment", () -> new PaymentValidator(), Void.class)
 *                 .method().name("validate").parameter(Input.class).end()
 *                 .end()
 *             .end()
 *         .stage("processing")  // Second stage
 *             .step("createOrder", () -> new OrderCreator(), OrderResult.class)
 *                 .method().name("create").parameter(Input.class).output(true).end()
 *                 .end()
 *             .end();
 *
 * // Access stage information
 * IRuntime<Order, OrderResult> runtime = builder.build();
 * // Stages are executed sequentially when runtime.execute() is called
 * }</pre>
 *
 * @param <InputType> the input type for the runtime containing this stage
 * @param <OutputType> the output type for the runtime containing this stage
 * @since 2.0.0-ALPHA01
 * @see IRuntime
 * @see IRuntimeStep
 * @see com.garganttua.core.runtime.dsl.IRuntimeStageBuilder
 */
public interface IRuntimeStage<InputType, OutputType> {

    /**
     * Returns the name of this stage.
     *
     * <p>
     * Stage names are used for identification, logging, and exception tracking.
     * They must be unique within a runtime definition.
     * </p>
     *
     * @return the stage name
     */
    String getStageName();

    /**
     * Retrieves a specific step within this stage by its name.
     *
     * @param stepName the name of the step to retrieve
     * @return the step with the specified name
     * @throws java.util.NoSuchElementException if no step with the given name exists
     * @see #getSteps()
     */
    IRuntimeStep<?, InputType, OutputType> getStep(String stepName);

    /**
     * Returns all steps in this stage as a map keyed by step name.
     *
     * <p>
     * The map preserves the order in which steps were defined and will be executed.
     * </p>
     *
     * @return an ordered map of step names to step instances
     * @see #getStep(String)
     */
    Map<String, IRuntimeStep<?, InputType, OutputType>> getSteps();

}
