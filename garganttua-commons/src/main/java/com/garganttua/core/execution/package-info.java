/**
 * Chain-of-responsibility pattern implementation for sequential task execution with fallback handling.
 *
 * <h2>Overview</h2>
 * <p>
 * This package implements the chain-of-responsibility design pattern, enabling sequential
 * processing of requests through a series of executors. Each executor can process the request,
 * pass it to the next executor, or handle failures with fallback logic.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.execution.IExecutor} - Single executor in the chain</li>
 *   <li>{@link com.garganttua.core.execution.IExecutorChain} - Chain of executors</li>
 *   <li><b>IExecutorContext</b> - Shared execution context (provided by implementations)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create execution chain
 * ExecutorChain<OrderRequest> chain = new ExecutorChain<>();
 *
 * // Add validator executor
 * chain.addExecutor((order, next) -> {
 *     if (order.getAmount() <= 0) {
 *         throw new ValidationException("Invalid amount");
 *     }
 *     next.execute(order);
 * });
 *
 * // Add enrichment executor with fallback
 * chain.addExecutor(
 *     (order, next) -> {
 *         // Try to enrich order with customer data
 *         CustomerData data = customerService.getData(order.getCustomerId());
 *         order.setCustomerData(data);
 *         next.execute(order);
 *     },
 *     (order, next) -> {
 *         // Fallback: use default customer data
 *         order.setCustomerData(CustomerData.getDefault());
 *         next.execute(order);
 *     }
 * );
 *
 * // Add persistence executor
 * chain.addExecutor((order, next) -> {
 *     orderRepository.save(order);
 *     next.execute(order);
 * });
 *
 * // Execute chain
 * OrderRequest order = new OrderRequest();
 * chain.execute(order);
 * }</pre>
 *
 * <h2>Fallback Handling</h2>
 * <pre>{@code
 * chain.addExecutor(
 *     // Primary executor
 *     (request, next) -> {
 *         Result result = externalService.call(request);
 *         request.setResult(result);
 *         next.execute(request);
 *     },
 *     // Fallback executor (called if primary throws exception)
 *     (request, next) -> {
 *         // Use cached result or default value
 *         Result cached = cache.get(request.getId());
 *         request.setResult(cached != null ? cached : Result.getDefault());
 *         next.execute(request);
 *     }
 * );
 * }</pre>
 *
 * <h2>Context Sharing</h2>
 * <pre>{@code
 * IExecutorContext context = new ExecutorContext();
 * context.setProperty("userId", "user123");
 *
 * chain.addExecutor((order, next, ctx) -> {
 *     String userId = ctx.getProperty("userId");
 *     order.setUserId(userId);
 *     ctx.setProperty("processed", true);
 *     next.execute(order);
 * });
 *
 * chain.execute(order, context);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Sequential processing through executor chain</li>
 *   <li>Optional fallback executors for error recovery</li>
 *   <li>Shared context for state management</li>
 *   <li>Early termination (break the chain)</li>
 *   <li>Exception propagation control</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><b>Request pipelines</b> - Validation, enrichment, transformation</li>
 *   <li><b>ETL processes</b> - Extract, transform, load with error handling</li>
 *   <li><b>Middleware chains</b> - Authentication, authorization, logging</li>
 *   <li><b>Resilient services</b> - Primary/fallback service calls</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.execution.IExecutor
 * @see com.garganttua.core.execution.IExecutorChain
 */
package com.garganttua.core.execution;
