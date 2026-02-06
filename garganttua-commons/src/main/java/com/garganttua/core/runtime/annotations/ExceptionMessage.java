package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a parameter to inject the exception message in fallback methods.
 *
 * <p>
 * The ExceptionMessage annotation is applied to String parameters in {@code @FallBack} methods
 * to inject the message of the exception that caused the main method to fail. This is a
 * convenience annotation when you only need the exception message, not the full exception object.
 * </p>
 *
 * <p>
 * The annotated parameter must be of type {@code String}. If no exception occurred or the
 * exception has no message, the parameter will be null.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Operation
 * public void validateOrder(@Input Order order) {
 *     if (order.getAmount() <= 0) {
 *         throw new IllegalArgumentException("Amount must be positive");
 *     }
 * }
 *
 * @FallBack
 * public void logValidationError(
 *         @ExceptionMessage String errorMessage,
 *         @Input Order order) {
 *
 *     // Log only the message
 *     logger.error("Validation failed for order {}: {}", order.getId(), errorMessage);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Exception
 * @see FallBack
 * @see com.garganttua.core.runtime.IRuntimeContext#getExceptionMessage()
 */
@Indexed
@Native
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionMessage {

}
