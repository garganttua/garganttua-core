/**
 * Runtime execution framework implementation for workflow orchestration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the Garganttua runtime execution
 * framework. It implements workflow orchestration with support for stages, steps, exception
 * handling, context management, and dynamic method invocation. The runtime enables
 * declarative and programmatic definition of complex business processes.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code Runtime} - Main runtime execution engine</li>
 *   <li>{@code RuntimeContext} - Execution context with variables and state</li>
 *   <li>{@code RuntimeContextFactory} - Factory for creating runtime contexts</li>
 *   <li>{@code RuntimeProcess} - Represents a runtime process instance</li>
 *   <li>{@code RuntimeResult} - Execution result with output and status</li>
 *   <li>{@code RuntimeStage} - Execution stage grouping related steps</li>
 *   <li>{@code RuntimeStep} - Individual execution step</li>
 * </ul>
 *
 * <h2>Exception Handling Classes</h2>
 * <ul>
 *   <li>{@code RuntimeStepCatch} - Exception catching configuration</li>
 *   <li>{@code RuntimeStepOnException} - Exception handler definition</li>
 *   <li>{@code RuntimeStepFallbackBinder} - Fallback method binding</li>
 *   <li>{@code RuntimeStepMethodBinder} - Step method binding</li>
 *   <li>{@code RuntimeStepExecutionTools} - Execution utilities</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Runtime Execution</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = OrderRequest.class, output = OrderResponse.class)
 * public class OrderProcessingRuntime {
 *
 *     @Stages
 *     @Step(order = 1)
 *     @Operation("validateOrder")
 *     public void validate(@Input OrderRequest request) {
 *         // Validation logic
 *     }
 *
 *     @Step(order = 2)
 *     @Operation("processPayment")
 *     public void payment(@Input OrderRequest request, @Output OrderResponse response) {
 *         // Payment processing
 *     }
 * }
 *
 * // Execute runtime
 * Runtime<OrderRequest, OrderResponse> runtime =
 *     RuntimeFactory.create(OrderProcessingRuntime.class);
 *
 * OrderRequest request = new OrderRequest();
 * RuntimeResult<OrderResponse> result = runtime.execute(request);
 *
 * if (result.isSuccess()) {
 *     OrderResponse response = result.getOutput();
 * }
 * }</pre>
 *
 * <h2>Usage Example: Programmatic Runtime</h2>
 * <pre>{@code
 * // Build runtime programmatically
 * Runtime<DataRequest, DataResponse> runtime =
 *     new RuntimeBuilder<DataRequest, DataResponse>()
 *         .input(DataRequest.class)
 *         .output(DataResponse.class)
 *
 *         .stage("processing")
 *             .step("fetchData")
 *                 .method(dataService, "fetch")
 *                     .parameter(0).input()
 *                     .done()
 *                 .done()
 *
 *             .step("transformData")
 *                 .method(transformer, "transform")
 *                     .parameter(0).variable("rawData")
 *                     .parameter(1).output()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         .build();
 *
 * // Execute
 * DataRequest request = new DataRequest();
 * RuntimeResult<DataResponse> result = runtime.execute(request);
 * }</pre>
 *
 * <h2>Runtime Lifecycle</h2>
 * <ol>
 *   <li><b>Initialization</b> - Runtime created and configured</li>
 *   <li><b>Context Creation</b> - Execution context initialized with input</li>
 *   <li><b>Stage Execution</b> - Stages executed in order</li>
 *   <li><b>Step Execution</b> - Steps within stage executed sequentially</li>
 *   <li><b>Method Invocation</b> - Step methods invoked with parameter binding</li>
 *   <li><b>Exception Handling</b> - Exceptions caught and handled if configured</li>
 *   <li><b>Completion</b> - Result created with output and status</li>
 * </ol>
 *
 * <h2>Context Management</h2>
 * <pre>{@code
 * // Access context in step method
 * @Step(order = 1)
 * @Operation("processData")
 * public void process(@Context RuntimeContext ctx, @Input DataRequest request) {
 *     // Store variable
 *     ctx.setVariable("userId", request.getUserId());
 *     ctx.setVariable("timestamp", System.currentTimeMillis());
 *
 *     // Retrieve variable
 *     String userId = ctx.getVariable("userId");
 *
 *     // Check variable existence
 *     if (ctx.hasVariable("timestamp")) {
 *         Long timestamp = ctx.getVariable("timestamp");
 *     }
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Declarative workflow definition via annotations</li>
 *   <li>Programmatic workflow construction via DSL</li>
 *   <li>Stage-based execution organization</li>
 *   <li>Sequential step execution</li>
 *   <li>Dynamic method invocation</li>
 *   <li>Parameter binding (input, output, context, variables)</li>
 *   <li>Exception handling and recovery</li>
 *   <li>Fallback strategies</li>
 *   <li>Conditional step execution</li>
 *   <li>Runtime variable management</li>
 *   <li>Execution result tracking</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.dsl} - Fluent builder implementations</li>
 *   <li>{@link com.garganttua.core.runtime.supply} - Runtime value suppliers</li>
 *   <li>{@link com.garganttua.core.runtime.resolver} - Parameter resolution</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.annotations
 * @see com.garganttua.core.runtime.dsl
 */
package com.garganttua.core.runtime;
