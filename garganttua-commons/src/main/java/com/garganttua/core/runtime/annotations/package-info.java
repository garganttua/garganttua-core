/**
 * Runtime execution framework annotations for declarative workflow definition.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides annotations for defining executable runtimes, workflow stages,
 * operations, exception handling, and data flow. These annotations enable declarative
 * configuration of complex business processes and execution pipelines.
 * </p>
 *
 * <h2>Core Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.annotations.RuntimeDefinition} - Defines a runtime with input/output types</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Stages} - Declares multiple execution stages</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Step} - Defines a single execution step</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Operation} - Marks methods as runtime operations</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Code} - Associates error codes with operations</li>
 * </ul>
 *
 * <h2>Context and Data Flow Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.annotations.Context} - Injects runtime execution context</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Input} - Injects runtime input data</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Output} - Injects runtime output data</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Variable} - Declares runtime variables</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Variables} - Declares multiple variables</li>
 * </ul>
 *
 * <h2>Exception Handling Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.annotations.OnException} - Defines exception handler methods</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Catch} - Declares exception catching behavior</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.FallBack} - Defines fallback logic</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.Exception} - Injects caught exception</li>
 *   <li>{@link com.garganttua.core.runtime.annotations.ExceptionMessage} - Injects exception message</li>
 * </ul>
 *
 * <h2>Conditional Execution</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.annotations.Condition} - Defines execution conditions</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Runtime</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = OrderRequest.class, output = OrderResponse.class)
 * public class OrderProcessingRuntime {
 *
 *     @Stages
 *     @Step(order = 1)
 *     @Operation("validateOrder")
 *     public void validate(@Input OrderRequest request, @Context RuntimeContext ctx) {
 *         // Validation logic
 *     }
 *
 *     @Step(order = 2)
 *     @Operation("processPayment")
 *     public void payment(@Input OrderRequest request, @Output OrderResponse response) {
 *         // Payment processing
 *     }
 *
 *     @Step(order = 3)
 *     @Operation("fulfillOrder")
 *     public void fulfill(@Output OrderResponse response) {
 *         // Fulfillment logic
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Exception Handling</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = DataRequest.class, output = DataResponse.class)
 * public class DataProcessingRuntime {
 *
 *     @Step(order = 1)
 *     @Operation("fetchData")
 *     @Catch(exceptions = {IOException.class, TimeoutException.class})
 *     public void fetch(@Input DataRequest request) throws IOException {
 *         // Data fetching that might fail
 *     }
 *
 *     @OnException(operation = "fetchData")
 *     @FallBack
 *     public void handleFetchError(
 *         @Exception Throwable error,
 *         @ExceptionMessage String message,
 *         @Output DataResponse response) {
 *
 *         response.setError(message);
 *         response.setStatus("FAILED");
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Variables and Context</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = UserInput.class, output = UserOutput.class)
 * @Variables({
 *     @Variable(name = "userId", type = String.class),
 *     @Variable(name = "timestamp", type = Long.class)
 * })
 * public class UserProcessingRuntime {
 *
 *     @Step(order = 1)
 *     @Operation("extractUserId")
 *     public void extract(@Input UserInput input, @Context RuntimeContext ctx) {
 *         String userId = input.getUserId();
 *         ctx.setVariable("userId", userId);
 *         ctx.setVariable("timestamp", System.currentTimeMillis());
 *     }
 *
 *     @Step(order = 2)
 *     @Operation("processUser")
 *     public void process(@Context RuntimeContext ctx, @Output UserOutput output) {
 *         String userId = ctx.getVariable("userId");
 *         Long timestamp = ctx.getVariable("timestamp");
 *         // Processing logic
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Conditional Execution</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = PaymentRequest.class, output = PaymentResponse.class)
 * public class PaymentRuntime {
 *
 *     @Step(order = 1)
 *     @Operation("validateAmount")
 *     @Condition("input.amount > 0")
 *     public void validate(@Input PaymentRequest request) {
 *         // Only executes if amount is positive
 *     }
 *
 *     @Step(order = 2)
 *     @Operation("applyDiscount")
 *     @Condition("input.customerType == 'PREMIUM'")
 *     public void discount(@Input PaymentRequest request) {
 *         // Only executes for premium customers
 *     }
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe input/output definitions</li>
 *   <li>Ordered step execution</li>
 *   <li>Automatic context injection</li>
 *   <li>Declarative exception handling</li>
 *   <li>Runtime variable management</li>
 *   <li>Conditional step execution</li>
 *   <li>Error code mapping</li>
 *   <li>Fallback strategies</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime
 * @see com.garganttua.core.runtime.dsl
 * @see com.garganttua.core.runtime.annotations.RuntimeDefinition
 * @see com.garganttua.core.runtime.annotations.Operation
 */
package com.garganttua.core.runtime.annotations;
