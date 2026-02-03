package com.garganttua.core.runtime;

import java.util.Optional;
import java.util.UUID;

/**
 * Central interface for executing multi-stage runtime workflows.
 *
 * <p>
 * IRuntime orchestrates the execution of multiple stages, each containing steps that process
 * the input and produce output. It supports exception handling, fallback mechanisms, shared
 * context management, and execution tracking.
 * </p>
 *
 * <p>
 * Runtimes can be defined either declaratively using annotations ({@link com.garganttua.core.runtime.annotations.RuntimeDefinition})
 * or programmatically using the DSL ({@link com.garganttua.core.runtime.dsl.IRuntimeBuilder}).
 * </p>
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>Steps</b> - Atomic units of work that transform data, executed sequentially</li>
 *   <li><b>Context</b> - Shared state accessible across all steps during execution</li>
 *   <li><b>Variables</b> - Named values stored in context for inter-step communication</li>
 *   <li><b>Exception Handling</b> - Catch blocks and fallback mechanisms for error recovery</li>
 * </ul>
 *
 * <h2>Usage Example - Annotation-Based</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderProcessingRuntime {
 *
 *     @Operation
 *     public void validateOrder(@Input Order order, @Context IRuntimeContext<Order, OrderResult> ctx) {
 *         if (order.getAmount() <= 0) {
 *             throw new IllegalArgumentException("Invalid amount");
 *         }
 *         ctx.setVariable("validatedAt", Instant.now());
 *     }
 *
 *     @Operation
 *     @Output
 *     public OrderResult processOrder(@Input Order order) {
 *         return new OrderResult(order.getId(), "PROCESSED");
 *     }
 *
 *     @FallBack
 *     @Output
 *     public OrderResult handleError(@Exception Throwable e) {
 *         return new OrderResult(null, "ERROR: " + e.getMessage());
 *     }
 * }
 *
 * // Execute the runtime
 * IRuntime<Order, OrderResult> runtime = runtimesBuilder.build().get("orderProcessing");
 * Optional<IRuntimeResult<Order, OrderResult>> result = runtime.execute(order);
 * }</pre>
 *
 * <h2>Usage Example - DSL-Based</h2>
 * <pre>{@code
 * IRuntimeBuilder<Order, OrderResult> builder = new RuntimesBuilder()
 *     .runtime("orderProcessing", Order.class, OrderResult.class)
 *         .step("validateOrder", () -> new OrderValidator(), Void.class)
 *             .method().name("validate").parameter(Input.class).end()
 *             .end()
 *         .step("processOrder", () -> new OrderProcessor(), OrderResult.class)
 *             .method().name("process").parameter(Input.class).output(true).end()
 *             .end();
 *
 * IRuntime<Order, OrderResult> runtime = builder.build();
 * Optional<IRuntimeResult<Order, OrderResult>> result = runtime.execute(order);
 * }</pre>
 *
 * @param <InputType> the input type that will be processed by this runtime
 * @param <OutputType> the output type produced by this runtime's execution
 * @since 2.0.0-ALPHA01
 * @see IRuntimeContext
 * @see IRuntimeResult
 * @see IRuntimeStep
 * @see com.garganttua.core.runtime.annotations.RuntimeDefinition
 * @see com.garganttua.core.runtime.dsl.IRuntimeBuilder
 */
public interface IRuntime<InputType, OutputType> {

    /**
     * The default success code returned when a runtime execution completes successfully.
     * This value is 0 and indicates no errors occurred during execution.
     */
    public static final int GENERIC_RUNTIME_SUCCESS_CODE = 0;

    /**
     * The default error code returned when a runtime execution fails.
     * This value is 50 and indicates a generic runtime error occurred.
     */
    public static final int GENERIC_RUNTIME_ERROR_CODE = 50;

    /**
     * Executes the runtime workflow with the provided input and an automatically generated UUID.
     *
     * <p>
     * This method orchestrates the execution of all configured stages and steps in sequence.
     * Each stage contains one or more steps that process the input, update the context, and
     * optionally produce output. If any step throws an uncaught exception and is configured
     * to abort on exception, the execution stops and returns the result with error information.
     * </p>
     *
     * @param input the input object to be processed by this runtime
     * @return an Optional containing the runtime result with output, timing metrics, and exception information;
     *         empty if execution could not complete
     * @throws RuntimeException if a critical error occurs that prevents execution
     * @see #execute(UUID, Object)
     * @see IRuntimeResult
     */
    Optional<IRuntimeResult<InputType, OutputType>> execute(InputType input) throws RuntimeException;

    /**
     * Executes the runtime workflow with the provided input and a specific UUID for tracking.
     *
     * <p>
     * This method is identical to {@link #execute(Object)} but allows you to provide a custom
     * UUID for execution tracking. This is useful for correlating runtime execution with external
     * systems, logs, or distributed tracing mechanisms.
     * </p>
     *
     * @param uuid the unique identifier for this execution instance
     * @param input the input object to be processed by this runtime
     * @return an Optional containing the runtime result with output, timing metrics, and exception information;
     *         empty if execution could not complete
     * @throws RuntimeException if a critical error occurs that prevents execution
     * @see #execute(Object)
     * @see IRuntimeResult#uuid()
     */
    Optional<IRuntimeResult<InputType, OutputType>> execute(UUID uuid, InputType input) throws RuntimeException;

}
