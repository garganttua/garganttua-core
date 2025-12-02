package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a method as a fallback handler for error recovery in a runtime step.
 *
 * <p>
 * The FallBack annotation is applied to methods that should be invoked when a step's
 * main {@code @Operation} method throws an uncaught exception. Fallback methods provide
 * a mechanism for graceful error handling and recovery, allowing the runtime to continue
 * execution or provide safe default outputs.
 * </p>
 *
 * <p>
 * Fallback methods can access exception information via {@code @Exception} parameters and
 * can produce output or store variables. They are typically used for error logging,
 * returning default values, or implementing circuit breaker patterns.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Invoked automatically when the main operation fails</li>
 *   <li>Can access exception details via {@code @Exception} parameters</li>
 *   <li>Can produce output using {@code @Output}</li>
 *   <li>Can store results in variables using {@code @Variable}</li>
 *   <li>Can handle specific exceptions using {@code @OnException}</li>
 *   <li>Has access to Input, Context, and other runtime parameters</li>
 * </ul>
 *
 * <h2>Usage Example - Basic Fallback</h2>
 * <pre>{@code
 * @Operation
 * public OrderResult processOrder(@Input Order order) {
 *     // May throw exception
 *     return expensiveOperation(order);
 * }
 *
 * @FallBack
 * @Output
 * public OrderResult handleProcessingError(@Exception Throwable e, @Input Order order) {
 *     // Fallback logic - return safe default
 *     logger.error("Processing failed: {}", e.getMessage());
 *     return new OrderResult(order.getId(), "FAILED");
 * }
 * }</pre>
 *
 * <h2>Usage Example - Conditional Fallback</h2>
 * <pre>{@code
 * @FallBack
 * @OnException(exception = IllegalArgumentException.class, fromStage = "validation")
 * @Output
 * public OrderResult handleValidationError(@Exception Throwable e) {
 *     // Only handles IllegalArgumentException from validation stage
 *     return new OrderResult(null, "VALIDATION_ERROR: " + e.getMessage());
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see Exception
 * @see OnException
 * @see Output
 * @see com.garganttua.core.runtime.IRuntimeStepFallbackBinder
 */
@Native
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FallBack {

}
