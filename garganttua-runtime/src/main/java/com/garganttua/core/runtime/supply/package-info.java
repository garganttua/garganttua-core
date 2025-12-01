/**
 * Runtime execution value suppliers and parameter resolution.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides supplier implementations for resolving runtime execution
 * parameters. It handles dynamic value resolution for input, output, context,
 * variables, and custom values during step method invocation.
 * </p>
 *
 * <h2>Supplier Types</h2>
 * <ul>
 *   <li><b>Input Supplier</b> - Provides runtime input data</li>
 *   <li><b>Output Supplier</b> - Provides runtime output data</li>
 *   <li><b>Context Supplier</b> - Provides runtime execution context</li>
 *   <li><b>Variable Supplier</b> - Provides runtime variables</li>
 *   <li><b>Exception Supplier</b> - Provides caught exceptions</li>
 *   <li><b>Value Supplier</b> - Provides fixed values</li>
 * </ul>
 *
 * <h2>Usage Example: Input Supplier</h2>
 * <pre>{@code
 * // Supply input to step method
 * InputSupplier<OrderRequest> inputSupplier =
 *     new InputSupplier<>(runtimeContext);
 *
 * OrderRequest input = inputSupplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Variable Supplier</h2>
 * <pre>{@code
 * // Supply variable from context
 * VariableSupplier<String> userIdSupplier =
 *     new VariableSupplier<>(runtimeContext, "userId", String.class);
 *
 * String userId = userIdSupplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Context Supplier</h2>
 * <pre>{@code
 * // Supply runtime context
 * ContextSupplier contextSupplier = new ContextSupplier(runtimeContext);
 *
 * RuntimeContext context = contextSupplier.get();
 * }</pre>
 *
 * <h2>Parameter Resolution</h2>
 * <p>
 * Suppliers are used to resolve method parameters based on annotations:
 * </p>
 * <ul>
 *   <li>{@code @Input} - Resolved by InputSupplier</li>
 *   <li>{@code @Output} - Resolved by OutputSupplier</li>
 *   <li>{@code @Context} - Resolved by ContextSupplier</li>
 *   <li>{@code @Variable} - Resolved by VariableSupplier</li>
 *   <li>{@code @Exception} - Resolved by ExceptionSupplier</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime
 * @see com.garganttua.core.runtime.annotations
 * @see com.garganttua.core.supply
 */
package com.garganttua.core.runtime.supply;
