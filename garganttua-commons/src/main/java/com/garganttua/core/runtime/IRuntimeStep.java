package com.garganttua.core.runtime;

import com.garganttua.core.execution.IExecutorChain;

/**
 * Represents an atomic unit of work within a runtime workflow stage.
 *
 * <p>
 * A runtime step is the smallest executable unit in a workflow. Each step typically performs
 * a single, well-defined operation such as validation, transformation, or persistence. Steps
 * are organized into stages and execute sequentially within their containing stage.
 * </p>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Atomic Operation</b> - Each step performs one specific task</li>
 *   <li><b>Context Access</b> - Steps can read/write to the shared runtime context</li>
 *   <li><b>Method Binding</b> - Steps bind to methods on target objects for execution</li>
 *   <li><b>Exception Handling</b> - Steps can define catch blocks and fallback methods</li>
 *   <li><b>Output Production</b> - Steps can optionally produce the runtime's final output</li>
 *   <li><b>Variables</b> - Steps can store intermediate results in named variables</li>
 * </ul>
 *
 * <h2>Step Execution Flow</h2>
 * <ol>
 *   <li>Step is invoked by the runtime executor</li>
 *   <li>Condition (if defined) is evaluated to determine if step should execute</li>
 *   <li>Main method is executed with injected parameters (Input, Context, Variables, etc.)</li>
 *   <li>If an exception occurs, catch blocks are checked for matching exception types</li>
 *   <li>If no catch block matches, fallback method is invoked (if defined)</li>
 *   <li>Step result (if any) is stored in context or as output</li>
 * </ol>
 *
 * <h2>Usage Example - Annotation-Based</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderRuntime {
 *
 *     // Simple operation step
 *     @Operation
 *     public void validateOrder(@Input Order order) {
 *         if (order.getAmount() <= 0) {
 *             throw new IllegalArgumentException("Invalid amount");
 *         }
 *     }
 *
 *     // Step that produces output
 *     @Operation
 *     @Output
 *     public OrderResult processOrder(@Input Order order) {
 *         return new OrderResult(order.getId(), "PROCESSED");
 *     }
 *
 *     // Step with exception handling
 *     @Operation
 *     @Catch(exception = IllegalArgumentException.class, code = 400)
 *     public void validateWithCatch(@Input Order order) {
 *         // ...
 *     }
 *
 *     // Step that stores result in variable
 *     @Operation
 *     @Variable(name = "validatedAt")
 *     public Instant recordValidation() {
 *         return Instant.now();
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example - DSL-Based</h2>
 * <pre>{@code
 * runtimeBuilder
 *     .stage("processing")
 *         .step("validateOrder", () -> new OrderValidator(), Void.class)
 *             .method()
 *                 .name("validate")
 *                 .parameter(Input.class)
 *                 .katch(IllegalArgumentException.class).code(400).end()
 *                 .end()
 *             .fallBack()
 *                 .name("handleError")
 *                 .parameter(Exception.class)
 *                 .output(true)
 *                 .end()
 *             .end()
 *         .step("processOrder", () -> new OrderProcessor(), OrderResult.class)
 *             .method()
 *                 .name("process")
 *                 .parameter(Input.class)
 *                 .output(true)
 *                 .end()
 *             .end();
 * }</pre>
 *
 * @param <ExecutionReturn> the return type of the step's main method
 * @param <InputType> the input type for the runtime containing this step
 * @param <OutputType> the output type for the runtime containing this step
 * @since 2.0.0-ALPHA01
 * @see IRuntimeStage
 * @see IRuntime
 * @see IRuntimeContext
 * @see com.garganttua.core.runtime.annotations.Operation
 * @see com.garganttua.core.runtime.dsl.IRuntimeStepBuilder
 */
public interface IRuntimeStep<ExecutionReturn, InputType, OutputType> {

    /**
     * Returns the name of this step.
     *
     * <p>
     * Step names are used for identification, logging, exception tracking, and
     * exception handler matching (via {@code @OnException} or DSL equivalents).
     * They must be unique within their containing stage.
     * </p>
     *
     * @return the step name
     */
    String getStepName();

    /**
     * Defines the execution logic for this step within the executor chain.
     *
     * <p>
     * This method is called during runtime initialization to configure the step's
     * execution behavior. The executor chain provides the infrastructure for invoking
     * the step's method, handling exceptions, and managing the context.
     * </p>
     *
     * <p>
     * This is an internal method primarily used by the runtime framework itself.
     * Application code typically does not need to call this method directly.
     * </p>
     *
     * @param chain the executor chain to configure with this step's execution logic
     * @see com.garganttua.core.execution.IExecutorChain
     */
    void defineExecutionStep(IExecutorChain<IRuntimeContext<InputType,OutputType>> chain);

}
