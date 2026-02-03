/**
 * Fluent builder APIs for programmatic runtime execution workflow construction.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides fluent DSL interfaces for building runtime execution workflows
 * programmatically. It offers a type-safe, readable alternative to annotation-based
 * runtime definitions, enabling dynamic workflow construction at runtime.
 * </p>
 *
 * <h2>Core Builder Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimeBuilder} - Main runtime workflow builder</li>
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimesBuilder} - Builder for multiple runtimes</li>
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimeStepBuilder} - Step configuration builder</li>
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimeStepMethodBuilder} - Method binding builder</li>
 * </ul>
 *
 * <h2>Exception Handling Builders</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimeStepCatchBuilder} - Exception catching configuration</li>
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimeStepFallbackBuilder} - Fallback strategy builder</li>
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimeStepOnExceptionBuilder} - Exception handler builder</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Runtime</h2>
 * <pre>{@code
 * IRuntime<OrderRequest, OrderResponse> orderRuntime =
 *     new RuntimesBuilder()
 *         .runtime("order", OrderRequest.class, OrderResponse.class)
 *             .step("validateOrder", () -> orderValidator, Void.class)
 *                 .method().name("validate").parameter(Input.class).end()
 *                 .end()
 *             .step("processPayment", () -> paymentService, Void.class)
 *                 .method().name("process").parameter(Input.class).end()
 *                 .end()
 *             .step("fulfillOrder", () -> fulfillmentService, OrderResponse.class)
 *                 .method().name("fulfill").parameter(Input.class).output(true).end()
 *                 .end()
 *         .build();
 *
 * // Execute runtime
 * Optional<IRuntimeResult<OrderRequest, OrderResponse>> result = orderRuntime.execute(orderRequest);
 * }</pre>
 *
 * <h2>Usage Example: Exception Handling</h2>
 * <pre>{@code
 * IRuntime<DataRequest, DataResponse> dataRuntime =
 *     new RuntimesBuilder()
 *         .runtime("data", DataRequest.class, DataResponse.class)
 *             .step("fetchData", () -> dataService, DataResponse.class)
 *                 .method()
 *                     .name("fetch")
 *                     .parameter(Input.class)
 *                     .katch(IOException.class).code(500).end()
 *                     .output(true)
 *                     .end()
 *                 .fallBack()
 *                     .name("fetchFromCache")
 *                     .parameter(Input.class)
 *                     .output(true)
 *                     .end()
 *                 .end()
 *         .build();
 * }</pre>
 *
 * <h2>Usage Example: Variables and Context</h2>
 * <pre>{@code
 * IRuntime<UserInput, UserOutput> userRuntime =
 *     new RuntimesBuilder()
 *         .runtime("user", UserInput.class, UserOutput.class)
 *             .step("extractUserId", () -> extractor, String.class)
 *                 .method()
 *                     .name("extract")
 *                     .parameter(Input.class)
 *                     .variable("userId")
 *                     .end()
 *                 .end()
 *             .step("processUser", () -> processor, UserOutput.class)
 *                 .method()
 *                     .name("process")
 *                     .parameter(Variable.class, "userId")
 *                     .parameter(Context.class)
 *                     .output(true)
 *                     .end()
 *                 .end()
 *         .build();
 * }</pre>
 *
 * <h2>Usage Example: Conditional Steps</h2>
 * <pre>{@code
 * IRuntime<PaymentRequest, PaymentResponse> paymentRuntime =
 *     new RuntimesBuilder()
 *         .runtime("payment", PaymentRequest.class, PaymentResponse.class)
 *             .step("validateAmount", () -> validator, Void.class)
 *                 .method()
 *                     .name("validate")
 *                     .parameter(Input.class)
 *                     .condition("amount > 0")
 *                     .end()
 *                 .end()
 *             .step("applyDiscount", () -> discountService, Void.class)
 *                 .method()
 *                     .name("apply")
 *                     .parameter(Input.class)
 *                     .condition("customerType == 'PREMIUM'")
 *                     .end()
 *                 .end()
 *             .step("processPayment", () -> paymentService, PaymentResponse.class)
 *                 .method()
 *                     .name("process")
 *                     .parameter(Input.class)
 *                     .output(true)
 *                     .end()
 *                 .end()
 *         .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe workflow construction</li>
 *   <li>Fluent method chaining</li>
 *   <li>Dynamic runtime creation</li>
 *   <li>Method binding with parameter mapping</li>
 *   <li>Exception handling configuration</li>
 *   <li>Variable and context management</li>
 *   <li>Conditional step execution</li>
 *   <li>Fallback strategies</li>
 *   <li>Reusable runtime definitions</li>
 * </ul>
 *
 * <h2>Builder Pattern</h2>
 * <p>
 * All builders follow these conventions:
 * </p>
 * <ul>
 *   <li>Method chaining for fluent configuration</li>
 *   <li>{@code end()} returns to parent builder</li>
 *   <li>{@code build()} creates the runtime</li>
 *   <li>Type parameters preserve type safety</li>
 *   <li>Clear builder hierarchy</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.dsl.IRuntimeBuilder
 * @see com.garganttua.core.runtime
 * @see com.garganttua.core.runtime.annotations
 * @see com.garganttua.core.dsl.IAutomaticBuilder
 */
package com.garganttua.core.runtime.dsl;
