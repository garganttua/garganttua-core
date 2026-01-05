/**
 * Execution chain framework implementation for sequential operation processing.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the execution chain framework.
 * It enables sequential execution of operations with support for chaining, result
 * mutation, and error handling in a pipeline pattern. Each executor in the chain
 * receives the current value and the chain itself, allowing for controlled flow.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code ExecutorChain} - Main execution chain implementation</li>
 *   <li>{@code IExecutor} - Functional interface for chain executors</li>
 *   <li>{@code IFallBackExecutor} - Fallback executor for error handling</li>
 * </ul>
 *
 * <h2>Usage Example: Integer Accumulator</h2>
 * <pre>{@code
 * // Create execution chain for integers
 * ExecutorChain<Integer> chain = new ExecutorChain<>();
 *
 * // Add executors that increment value
 * chain.addExecutor((i, ch) -> {
 *     i = i + 1;
 *     ch.execute(i);
 * });
 * chain.addExecutor((i, ch) -> {
 *     i++;
 *     ch.execute(i);
 * });
 * chain.addExecutor((i, ch) -> {
 *     i++;
 *     ch.execute(i);
 * });
 *
 * // Execute chain starting with 0
 * chain.execute(0);
 * // Final value: 3
 * }</pre>
 *
 * <h2>Usage Example: String Builder Chain</h2>
 * <pre>{@code
 * StringBuilder sb = new StringBuilder();
 * ExecutorChain<StringBuilder> chain = new ExecutorChain<>();
 *
 * chain.addExecutor((s, ch) -> {
 *     s.append("This ");
 *     ch.execute(s);
 * });
 * chain.addExecutor((s, ch) -> {
 *     s.append("is ");
 *     ch.execute(s);
 * });
 * chain.addExecutor((s, ch) -> {
 *     s.append("test");
 *     ch.execute(s);
 * });
 *
 * chain.execute(sb);
 * // Result: "This is test"
 * }</pre>
 *
 * <h2>Usage Example: Mathematical Operations</h2>
 * <pre>{@code
 * Integer value = 0;
 * ExecutorChain<Integer> chain = new ExecutorChain<>();
 *
 * chain.addExecutor((i, ch) -> {
 *     i *= 2;  // 0
 *     ch.execute(i);
 * });
 * chain.addExecutor((i, ch) -> {
 *     i++;     // 1
 *     ch.execute(i);
 * });
 * chain.addExecutor((i, ch) -> {
 *     i *= 2;  // 2
 *     ch.execute(i);
 * });
 * chain.addExecutor((i, ch) -> {
 *     i++;     // 3
 *     ch.execute(i);
 * });
 * chain.addExecutor((i, ch) -> {
 *     i *= 2;  // 6
 *     ch.execute(i);
 * });
 *
 * chain.execute(value);
 * // Final value: 6
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Sequential operation execution</li>
 *   <li>Value mutation through chain</li>
 *   <li>Type-safe execution chain</li>
 *   <li>Operation composition</li>
 *   <li>Fallback executor support</li>
 *   <li>Exception handling with rethrow option</li>
 *   <li>Reusable chains</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.execution;
