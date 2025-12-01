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
 *   <li>{@link com.garganttua.core.runtime.dsl.IRuntimeStageBuilder} - Stage configuration builder</li>
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
 *     new RuntimeBuilder<OrderRequest, OrderResponse>()
 *         .input(OrderRequest.class)
 *         .output(OrderResponse.class)
 *
 *         .stage("validation")
 *             .step("validateOrder")
 *                 .method(orderService, "validate")
 *                     .parameter(0).input()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         .stage("processing")
 *             .step("processPayment")
 *                 .method(paymentService, "process")
 *                     .parameter(0).input()
 *                     .parameter(1).output()
 *                     .done()
 *                 .done()
 *             .step("fulfillOrder")
 *                 .method(fulfillmentService, "fulfill")
 *                     .parameter(0).output()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         .build();
 *
 * // Execute runtime
 * OrderResponse response = orderRuntime.execute(orderRequest);
 * }</pre>
 *
 * <h2>Usage Example: Exception Handling</h2>
 * <pre>{@code
 * IRuntime<DataRequest, DataResponse> dataRuntime =
 *     new RuntimeBuilder<DataRequest, DataResponse>()
 *         .input(DataRequest.class)
 *         .output(DataResponse.class)
 *
 *         .stage("dataProcessing")
 *             .step("fetchData")
 *                 .method(dataService, "fetch")
 *                     .parameter(0).input()
 *                     .done()
 *                 .catchException(IOException.class)
 *                     .onException()
 *                         .method(errorHandler, "handleIOError")
 *                             .parameter(0).exception()
 *                             .parameter(1).output()
 *                             .done()
 *                         .done()
 *                     .fallback()
 *                         .method(dataService, "fetchFromCache")
 *                             .parameter(0).input()
 *                             .done()
 *                         .done()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         .build();
 * }</pre>
 *
 * <h2>Usage Example: Variables and Context</h2>
 * <pre>{@code
 * IRuntime<UserInput, UserOutput> userRuntime =
 *     new RuntimeBuilder<UserInput, UserOutput>()
 *         .input(UserInput.class)
 *         .output(UserOutput.class)
 *
 *         .variable("userId", String.class)
 *         .variable("timestamp", Long.class)
 *
 *         .stage("extraction")
 *             .step("extractUserId")
 *                 .method(extractor, "extract")
 *                     .parameter(0).input()
 *                     .parameter(1).context()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         .stage("processing")
 *             .step("processUser")
 *                 .method(processor, "process")
 *                     .parameter(0).variable("userId")
 *                     .parameter(1).variable("timestamp")
 *                     .parameter(2).output()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         .build();
 * }</pre>
 *
 * <h2>Usage Example: Conditional Steps</h2>
 * <pre>{@code
 * IRuntime<PaymentRequest, PaymentResponse> paymentRuntime =
 *     new RuntimeBuilder<PaymentRequest, PaymentResponse>()
 *         .input(PaymentRequest.class)
 *         .output(PaymentResponse.class)
 *
 *         .stage("processing")
 *             .step("validateAmount")
 *                 .condition(ctx -> ctx.getInput().getAmount() > 0)
 *                 .method(validator, "validate")
 *                     .parameter(0).input()
 *                     .done()
 *                 .done()
 *
 *             .step("applyDiscount")
 *                 .condition(ctx -> "PREMIUM".equals(ctx.getInput().getCustomerType()))
 *                 .method(discountService, "apply")
 *                     .parameter(0).input()
 *                     .done()
 *                 .done()
 *
 *             .step("processPayment")
 *                 .method(paymentService, "process")
 *                     .parameter(0).input()
 *                     .parameter(1).output()
 *                     .done()
 *                 .done()
 *             .done()
 *
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
 *   <li>{@code done()} returns to parent builder</li>
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
