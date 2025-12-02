package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a method as a runtime operation within a step.
 *
 * <p>
 * The Operation annotation is the primary annotation for defining executable steps in a
 * runtime workflow. Each {@code @Operation} method represents an atomic unit of work that
 * processes input, updates context, and optionally produces output or stores variables.
 * </p>
 *
 * <p>
 * Operation methods are executed sequentially within their containing stage. They can access
 * the runtime input, context, variables, and other runtime resources via parameter annotations.
 * Exception handling can be configured using {@code @Catch} annotations or {@code @FallBack} methods.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Defines executable step logic</li>
 *   <li>Supports parameter injection (Input, Context, Variables, etc.)</li>
 *   <li>Can produce output using {@code @Output}</li>
 *   <li>Can store results in variables using {@code @Variable}</li>
 *   <li>Supports exception handling via {@code @Catch} and {@code @FallBack}</li>
 *   <li>Configurable abort behavior on uncaught exceptions</li>
 * </ul>
 *
 * <h2>Usage Example - Simple Operation</h2>
 * <pre>{@code
 * @Operation
 * public void validateOrder(@Input Order order) {
 *     if (order.getAmount() <= 0) {
 *         throw new IllegalArgumentException("Invalid amount");
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example - Operation with Output</h2>
 * <pre>{@code
 * @Operation
 * @Output
 * public OrderResult processOrder(@Input Order order) {
 *     // Process order and return result
 *     return new OrderResult(order.getId(), "PROCESSED");
 * }
 * }</pre>
 *
 * <h2>Usage Example - Operation with Variable Storage</h2>
 * <pre>{@code
 * @Operation
 * @Variable(name = "processedAt")
 * public Instant recordProcessingTime(@Input Order order) {
 *     return Instant.now();
 * }
 * }</pre>
 *
 * <h2>Usage Example - Operation with Exception Handling</h2>
 * <pre>{@code
 * @Operation
 * @Catch(exception = IllegalArgumentException.class, code = 400)
 * public void validateWithCatch(@Input Order order) {
 *     if (order.getAmount() <= 0) {
 *         throw new IllegalArgumentException("Invalid amount");
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example - Operation that Aborts on Error</h2>
 * <pre>{@code
 * @Operation(abortOnUncatchedException = true)
 * public void criticalValidation(@Input Order order) {
 *     // If this throws uncaught exception, runtime execution stops
 *     performCriticalCheck(order);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see RuntimeDefinition
 * @see Input
 * @see Output
 * @see Variable
 * @see Context
 * @see Catch
 * @see FallBack
 * @see com.garganttua.core.runtime.IRuntimeStep
 */
@Native
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Operation {

    /**
     * Whether to abort runtime execution if this operation throws an uncaught exception.
     *
     * <p>
     * When true, if this operation throws an exception that is not caught by {@code @Catch}
     * annotations and has no {@code @FallBack} method, the entire runtime execution will
     * terminate immediately. The exception will be recorded and marked as aborting.
     * </p>
     *
     * <p>
     * When false (default), uncaught exceptions are recorded but do not stop execution.
     * Subsequent steps and stages will continue to execute normally.
     * </p>
     *
     * @return true to abort on uncaught exceptions, false to continue execution
     */
    boolean abortOnUncatchedException() default false;

}
