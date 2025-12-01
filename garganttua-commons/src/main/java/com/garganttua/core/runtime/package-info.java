/**
 * Runtime workflow orchestration framework for executing multi-stage, multi-step processes.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a sophisticated workflow engine for orchestrating complex business
 * processes. It supports multi-stage execution with conditional steps, exception handling,
 * fallback mechanisms, and both annotation-based and programmatic definitions.
 * </p>
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li><b>Runtime</b> - Orchestrates execution of stages and steps</li>
 *   <li><b>Stage</b> - Group of related steps executed sequentially</li>
 *   <li><b>Step</b> - Atomic unit of work within a stage</li>
 *   <li><b>Context</b> - Shared state accessible throughout execution</li>
 *   <li><b>Fallback</b> - Exception handlers for error recovery</li>
 * </ul>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.IRuntime} - Main workflow orchestrator</li>
 *   <li>{@link com.garganttua.core.runtime.IRuntimeContext} - Execution context with shared state</li>
 *   <li>{@link com.garganttua.core.runtime.IRuntimeResult} - Result of runtime execution</li>
 *   <li>{@link com.garganttua.core.runtime.IStage} - Stage definition</li>
 *   <li>{@link com.garganttua.core.runtime.IStep} - Step definition</li>
 * </ul>
 *
 * <h2>Annotation-Based Definition</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * @Stages({
 *     @Stage(name = "validation", steps = {
 *         @Step(method = "validateOrder"),
 *         @Step(method = "checkInventory")
 *     }),
 *     @Stage(name = "processing", steps = {
 *         @Step(method = "processPayment"),
 *         @Step(method = "shipOrder")
 *     }),
 *     @Stage(name = "notification", steps = {
 *         @Step(method = "sendConfirmation")
 *     })
 * })
 * public class OrderProcessingRuntime {
 *
 *     public void validateOrder(@Input Order order, @Context IRuntimeContext ctx) {
 *         if (order.getAmount() <= 0) {
 *             throw new ValidationException("Invalid order amount");
 *         }
 *     }
 *
 *     public void checkInventory(@Input Order order) {
 *         // Check if items are in stock
 *     }
 *
 *     public void processPayment(@Input Order order, @Context IRuntimeContext ctx) {
 *         // Process payment
 *         ctx.setContextProperty("paymentId", "PAY-123");
 *     }
 *
 *     @FallBack
 *     public void handleFailure(@Exception Throwable ex, @Context IRuntimeContext ctx) {
 *         // Rollback and notify
 *         System.err.println("Order processing failed: " + ex.getMessage());
 *     }
 * }
 *
 * // Execute
 * IRuntime<Order, OrderResult> runtime = new RuntimeBuilder<>(OrderProcessingRuntime.class).build();
 * IRuntimeResult<OrderResult> result = runtime.execute(myOrder);
 * }</pre>
 *
 * <h2>Programmatic Definition</h2>
 * <pre>{@code
 * IRuntime<Order, OrderResult> runtime = new RuntimeBuilder<Order, OrderResult>()
 *     .stage("validation")
 *         .step("validate", this::validateOrder)
 *         .step("checkInventory", this::checkInventory)
 *         .end()
 *     .stage("processing")
 *         .step("payment", this::processPayment)
 *         .step("shipping", this::shipOrder)
 *         .end()
 *     .fallback(this::handleFailure)
 *     .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Multi-stage workflow execution</li>
 *   <li>Conditional step execution</li>
 *   <li>Exception handling with fallbacks</li>
 *   <li>Shared context for state management</li>
 *   <li>Before/after hooks</li>
 *   <li>Skip conditions for steps</li>
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
