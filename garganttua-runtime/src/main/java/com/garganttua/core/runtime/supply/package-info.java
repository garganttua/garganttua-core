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
 * <h2>Usage Example: Input Parameter</h2>
 * <pre>{@code
 * // Using @Input annotation in step method
 * @Step
 * @Named("output-step")
 * public class DummyRuntimeProcessOutputStep {
 *
 *     @Operation
 *     String method(@Input String input) {
 *         // Input is automatically supplied from runtime context
 *         return input + "-processed";
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Variable Parameter</h2>
 * <pre>{@code
 * // Using @Variable annotation to access runtime variables
 * @Step
 * @Named("step-one")
 * public class StepOne {
 *
 *     @Operation
 *     @Variable(name = "step-one-returned")
 *     String method(
 *             @Input String input,
 *             @Variable(name = "step-one-variable") String variable) {
 *         // Variables are supplied from runtime context
 *         return input + "-step-one-processed-" + variable;
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Context and Fixed Value Parameters</h2>
 * <pre>{@code
 * // Using multiple parameter types
 * @Step
 * @Named("output-step")
 * public class DummyRuntimeProcessOutputStep {
 *
 *     @Operation
 *     String method(
 *             @Input String input,
 *             @Fixed(valueString = "fixed-value-in-method") String fixedValue,
 *             @Variable(name = "variable") String variable,
 *             @Context IRuntimeContext<String, String> context) {
 *         // Input, fixed values, variables, and context are all supplied
 *         return input + "-processed-" + fixedValue + "-" + variable;
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Exception Parameters in Fallback</h2>
 * <pre>{@code
 * // Using exception-related suppliers in fallback methods
 * @Step
 * @Named("output-step")
 * public class DummyRuntimeProcessOutputStep {
 *
 *     @FallBack
 *     @OnException(exception = DiException.class)
 *     String fallbackMethod(
 *             @Input String input,
 *             @Exception DiException exception,
 *             @Code Integer code,
 *             @ExceptionMessage String exceptionMessage,
 *             @Context IRuntimeContext<String, String> context) {
 *         // Exception, code, and message are supplied from runtime context
 *         return input + "-fallback-" + code + "-" + exceptionMessage;
 *     }
 * }
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
