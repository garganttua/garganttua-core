/**
 * Runtime parameter and dependency resolution strategies.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides implementation classes for resolving runtime execution
 * parameters. It handles annotation-based parameter resolution, type matching,
 * and value extraction from the runtime context.
 * </p>
 *
 * <h2>Resolution Strategies</h2>
 * <ul>
 *   <li><b>Input Resolution</b> - Resolve {@code @Input} annotated parameters</li>
 *   <li><b>Output Resolution</b> - Resolve {@code @Output} annotated parameters</li>
 *   <li><b>Context Resolution</b> - Resolve {@code @Context} annotated parameters</li>
 *   <li><b>Variable Resolution</b> - Resolve {@code @Variable} annotated parameters</li>
 *   <li><b>Exception Resolution</b> - Resolve {@code @Exception} annotated parameters</li>
 *   <li><b>Code Resolution</b> - Resolve {@code @Code} annotated parameters</li>
 *   <li><b>Fixed Value Resolution</b> - Resolve {@code @Fixed} annotated parameters</li>
 *   <li><b>Exception Message Resolution</b> - Resolve {@code @ExceptionMessage} annotated parameters</li>
 * </ul>
 *
 * <h2>Usage Example: Input and Variable Resolution</h2>
 * <pre>{@code
 * @Step
 * @Named("step-one")
 * public class StepOne {
 *     @Operation
 *     @Variable(name = "step-one-returned")
 *     String method(
 *             @Input String input,                              // Resolved from runtime input
 *             @Variable(name = "step-one-variable") String variable) // Resolved from runtime variables
 *             throws DiException {
 *         return input + "-step-one-processed-" + variable;
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Context and Fixed Value Resolution</h2>
 * <pre>{@code
 * @Step
 * @Named("output-step")
 * public class DummyRuntimeProcessOutputStep {
 *     @Operation
 *     String method(
 *             @Input String input,                                      // Resolved from runtime input
 *             @Fixed(valueString = "fixed-value-in-method") String fixedValue, // Resolved to fixed value
 *             @Variable(name = "variable") String variable,             // Resolved from runtime variables
 *             @Context IRuntimeContext<String, String> context)         // Resolved to runtime context
 *             throws DiException {
 *         return input + "-processed-" + fixedValue + "-" + variable;
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Exception Resolution in Fallback</h2>
 * <pre>{@code
 * @Step
 * @Named("output-step")
 * public class DummyRuntimeProcessOutputStep {
 *     @FallBack
 *     @OnException(exception = DiException.class)
 *     String fallbackMethod(
 *             @Input String input,                    // Resolved from runtime input
 *             @Exception DiException exception,       // Resolved from caught exception
 *             @Code Integer code,                     // Resolved from exception code
 *             @ExceptionMessage String exceptionMessage, // Resolved from exception message
 *             @Context IRuntimeContext<String, String> context) { // Resolved to runtime context
 *         return input + "-fallback-" + code + "-" + exceptionMessage;
 *     }
 * }
 * }</pre>
 *
 * <h2>Parameter Detection</h2>
 * <p>
 * Resolvers scan step method parameters for annotations:
 * </p>
 * <ol>
 *   <li>Detect parameter annotations (@Input, @Variable, @Exception, etc.)</li>
 *   <li>Determine resolution strategy based on annotation type</li>
 *   <li>Extract value from runtime context</li>
 *   <li>Perform type conversion if needed</li>
 *   <li>Inject resolved value into method parameter</li>
 * </ol>
 *
 * <h2>Type Matching</h2>
 * <p>
 * Resolvers ensure type compatibility:
 * </p>
 * <ul>
 *   <li>Input type matches runtime input type (String in examples)</li>
 *   <li>Output type matches runtime output type (String in examples)</li>
 *   <li>Variable type matches declared type (String, Integer, etc.)</li>
 *   <li>Exception type is assignable from thrown exception (DiException, CustomException)</li>
 *   <li>Context type matches runtime context generic types</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime
 * @see com.garganttua.core.runtime.annotations
 */
package com.garganttua.core.runtime.resolver;
