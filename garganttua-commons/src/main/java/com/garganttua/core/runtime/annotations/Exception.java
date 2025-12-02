package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a parameter to inject exception information in fallback methods.
 *
 * <p>
 * The Exception annotation is applied to parameters in {@code @FallBack} methods to inject
 * the exception that caused the main method to fail. This allows fallback methods to access
 * exception details for error handling, logging, or recovery logic.
 * </p>
 *
 * <p>
 * The annotated parameter can be of type {@code Throwable} or any specific exception type.
 * If a specific type is used, it will only receive exceptions of that type or its subtypes.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Operation
 * public OrderResult processOrder(@Input Order order) {
 *     if (order.getAmount() <= 0) {
 *         throw new IllegalArgumentException("Invalid amount");
 *     }
 *     // ... processing logic that may fail
 *     return result;
 * }
 *
 * @FallBack
 * @Output
 * public OrderResult handleError(
 *         @Exception Throwable e,
 *         @Input Order order,
 *         @Context IRuntimeContext<Order, OrderResult> ctx) {
 *
 *     // Log exception
 *     logger.error("Failed to process order: {}", e.getMessage(), e);
 *
 *     // Return safe default
 *     return new OrderResult(order.getId(), "FAILED: " + e.getMessage());
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see FallBack
 * @see OnException
 * @see ExceptionMessage
 * @see Catch
 */
@Native
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Exception {

}
