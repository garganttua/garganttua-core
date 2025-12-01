/**
 * Fluent builder API implementations for runtime execution workflow construction.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of the fluent DSL interfaces defined
 * in garganttua-commons for building runtime execution workflows. It implements the
 * builder pattern to provide a type-safe, readable API for programmatic workflow
 * construction.
 * </p>
 *
 * <h2>Implementation Classes</h2>
 * <p>
 * This package contains implementations of the builder interfaces from
 * {@link com.garganttua.core.runtime.dsl} (commons package).
 * </p>
 *
 * <h2>Usage Example: Complete Runtime Builder</h2>
 * <pre>{@code
 * Runtime<OrderRequest, OrderResponse> runtime =
 *     new RuntimeBuilder<OrderRequest, OrderResponse>()
 *         .input(OrderRequest.class)
 *         .output(OrderResponse.class)
 *
 *         // Define variables
 *         .variable("orderId", String.class)
 *         .variable("timestamp", Long.class)
 *
 *         // Validation stage
 *         .stage("validation")
 *             .step("validateRequest")
 *                 .method(validator, "validate")
 *                     .parameter(0).input()
 *                     .done()
 *                 .catchException(ValidationException.class)
 *                     .onException()
 *                         .method(errorHandler, "handleValidation")
 *                             .parameter(0).exception()
 *                             .parameter(1).output()
 *                             .done()
 *                         .done()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         // Processing stage
 *         .stage("processing")
 *             .step("extractOrderId")
 *                 .method(extractor, "extract")
 *                     .parameter(0).input()
 *                     .parameter(1).context()
 *                     .done()
 *                 .done()
 *
 *             .step("processPayment")
 *                 .method(paymentService, "process")
 *                     .parameter(0).variable("orderId")
 *                     .parameter(1).input()
 *                     .parameter(2).output()
 *                     .done()
 *                 .catchException(PaymentException.class)
 *                     .fallback()
 *                         .method(paymentService, "refund")
 *                             .parameter(0).variable("orderId")
 *                             .done()
 *                         .done()
 *                     .done()
 *                 .done()
 *
 *             .step("fulfillOrder")
 *                 .condition(ctx -> ctx.getOutput().isPaymentSuccessful())
 *                 .method(fulfillmentService, "fulfill")
 *                     .parameter(0).variable("orderId")
 *                     .parameter(1).output()
 *                     .done()
 *                 .done()
 *             .done()
 *
 *         .build();
 *
 * // Execute runtime
 * OrderRequest request = new OrderRequest();
 * RuntimeResult<OrderResponse> result = runtime.execute(request);
 * }</pre>
 *
 * <h2>Usage Example: Step Method Builder</h2>
 * <pre>{@code
 * // Build step with method binding
 * RuntimeStepMethodBuilder stepBuilder = new RuntimeStepMethodBuilder()
 *     .target(userService)
 *     .method("updateUser")
 *     .parameter(0)
 *         .input()
 *         .done()
 *     .parameter(1)
 *         .variable("userId")
 *         .done()
 *     .parameter(2)
 *         .output()
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Exception Handling Builder</h2>
 * <pre>{@code
 * // Build step with exception handling
 * RuntimeStepBuilder stepBuilder = new RuntimeStepBuilder()
 *     .name("processData")
 *     .method(dataService, "process")
 *         .parameter(0).input()
 *         .done()
 *     .catchException(IOException.class)
 *         .onException()
 *             .method(errorHandler, "handleIO")
 *                 .parameter(0).exception()
 *                 .parameter(1).exceptionMessage()
 *                 .done()
 *             .done()
 *         .fallback()
 *             .method(dataService, "processFromCache")
 *                 .parameter(0).input()
 *                 .done()
 *             .done()
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe workflow construction</li>
 *   <li>Stage and step organization</li>
 *   <li>Method binding with parameter mapping</li>
 *   <li>Exception handling configuration</li>
 *   <li>Fallback strategy definition</li>
 *   <li>Variable declaration and usage</li>
 *   <li>Conditional step execution</li>
 *   <li>Context-aware parameter resolution</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.dsl
 * @see com.garganttua.core.runtime
 */
package com.garganttua.core.runtime.dsl;
