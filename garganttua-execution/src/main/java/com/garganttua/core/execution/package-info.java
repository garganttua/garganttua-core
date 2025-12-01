/**
 * Execution chain framework implementation for sequential operation processing.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the execution chain framework.
 * It enables sequential execution of operations with support for chaining, result
 * passing, and error handling in a pipeline pattern.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code ExecutorChain} - Main execution chain implementation</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Chain</h2>
 * <pre>{@code
 * // Create execution chain
 * ExecutorChain<String, String> chain = new ExecutorChain<>();
 *
 * // Add executors
 * chain.add(input -> input.trim());
 * chain.add(input -> input.toUpperCase());
 * chain.add(input -> input.replaceAll("\\s+", "_"));
 *
 * // Execute chain
 * String result = chain.execute("  hello world  ");
 * // Result: "HELLO_WORLD"
 * }</pre>
 *
 * <h2>Usage Example: Typed Chain</h2>
 * <pre>{@code
 * // Create chain with different input/output types
 * ExecutorChain<UserRequest, UserResponse> chain = new ExecutorChain<>();
 *
 * chain.add(request -> validate(request));
 * chain.add(validated -> transform(validated));
 * chain.add(transformed -> persist(transformed));
 * chain.add(persisted -> createResponse(persisted));
 *
 * UserResponse response = chain.execute(userRequest);
 * }</pre>
 *
 * <h2>Usage Example: Error Handling</h2>
 * <pre>{@code
 * ExecutorChain<Data, Result> chain = new ExecutorChain<>();
 *
 * chain.add(data -> {
 *     try {
 *         return processData(data);
 *     } catch (Exception e) {
 *         return handleError(e);
 *     }
 * });
 *
 * Result result = chain.execute(inputData);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Sequential operation execution</li>
 *   <li>Result passing between operations</li>
 *   <li>Type-safe execution chain</li>
 *   <li>Operation composition</li>
 *   <li>Error propagation</li>
 *   <li>Reusable chains</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.execution;
