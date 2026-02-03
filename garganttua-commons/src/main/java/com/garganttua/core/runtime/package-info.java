/**
 * Runtime workflow orchestration framework for executing multi-step processes.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a workflow engine for orchestrating business processes.
 * It supports sequential step execution with conditional logic, exception handling,
 * fallback mechanisms, and both annotation-based and programmatic definitions.
 * </p>
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>Runtime</b> - Orchestrates execution of steps</li>
 *   <li><b>Step</b> - Atomic unit of work in the workflow</li>
 *   <li><b>Context</b> - Shared state accessible throughout execution</li>
 *   <li><b>Fallback</b> - Exception handlers for error recovery</li>
 * </ul>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.IRuntime} - Main workflow orchestrator</li>
 *   <li>{@link com.garganttua.core.runtime.IRuntimeContext} - Execution context with shared state</li>
 *   <li>{@link com.garganttua.core.runtime.IRuntimeResult} - Result of runtime execution</li>
 *   <li>{@link com.garganttua.core.runtime.IRuntimeStep} - Step definition</li>
 * </ul>
 *
 * <h2>Annotation-Based Definition</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderProcessingRuntime {
 *
 *     @Steps
 *     private List<Class<?>> steps = Arrays.asList(
 *         ValidationStep.class,
 *         ProcessingStep.class,
 *         NotificationStep.class
 *     );
 *
 *     @FallBack
 *     public void handleFailure(@Exception Throwable ex, @Context IRuntimeContext ctx) {
 *         System.err.println("Order processing failed: " + ex.getMessage());
 *     }
 * }
 *
 * @Step
 * public class ValidationStep {
 *     @Operation
 *     public void validateOrder(@Input Order order) {
 *         if (order.getAmount() <= 0) {
 *             throw new ValidationException("Invalid order amount");
 *         }
 *     }
 * }
 *
 * // Execute
 * IRuntime<Order, OrderResult> runtime = runtimesBuilder.build().get("orderProcessing");
 * IRuntimeResult<OrderResult> result = runtime.execute(myOrder);
 * }</pre>
 *
 * <h2>Programmatic Definition</h2>
 * <pre>{@code
 * IRuntime<Order, OrderResult> runtime = new RuntimesBuilder()
 *     .runtime("order", Order.class, OrderResult.class)
 *         .step("validate", () -> new OrderValidator(), Void.class)
 *             .method().name("validate").parameter(Input.class).end()
 *             .end()
 *         .step("process", () -> new OrderProcessor(), OrderResult.class)
 *             .method().name("process").parameter(Input.class).output(true).end()
 *             .end()
 *     .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Sequential step execution</li>
 *   <li>Conditional step execution</li>
 *   <li>Exception handling with fallbacks</li>
 *   <li>Shared context for state management</li>
 *   <li>Variable storage for inter-step communication</li>
 *   <li>Integration with DI container</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.annotations} - Annotations for declarative runtime definition</li>
 *   <li>{@link com.garganttua.core.runtime.dsl} - Fluent builder APIs for programmatic runtime creation</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.IRuntime
 * @see com.garganttua.core.runtime.IRuntimeContext
 * @see com.garganttua.core.runtime.annotations
 */
package com.garganttua.core.runtime;
