package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a fallback method to handle exceptions from specific steps or stages.
 *
 * <p>
 * The OnException annotation is applied to {@code @FallBack} methods to specify which
 * exceptions should trigger the fallback. It enables cross-step exception handling by
 * allowing fallbacks to respond to exceptions from other steps in the same or different stages.
 * </p>
 *
 * <p>
 * When an exception matching the specified criteria occurs, the fallback method is invoked.
 * The criteria include the exception type and optionally the source stage and step names.
 * Empty strings for stage/step names act as wildcards, matching any stage or step.
 * </p>
 *
 * <h2>Usage Example - Handle Specific Exception</h2>
 * <pre>{@code
 * @FallBack
 * @OnException(exception = IllegalArgumentException.class)
 * @Output
 * public OrderResult handleValidationError(@Exception Throwable e) {
 *     // Handles IllegalArgumentException from any step
 *     return new OrderResult(null, "VALIDATION_ERROR: " + e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Usage Example - Handle Exception from Specific Step</h2>
 * <pre>{@code
 * @FallBack
 * @OnException(
 *     exception = IllegalArgumentException.class,
 *     fromStage = "validation",
 *     fromStep = "validateAmount"
 * )
 * @Output
 * public OrderResult handleAmountValidationError(@Exception Throwable e) {
 *     // Only handles IllegalArgumentException from validation.validateAmount
 *     return new OrderResult(null, "INVALID_AMOUNT: " + e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Usage Example - Handle Exceptions from Stage</h2>
 * <pre>{@code
 * @FallBack
 * @OnException(exception = Exception.class, fromStage = "database")
 * @Output
 * public OrderResult handleDatabaseErrors(@Exception Throwable e) {
 *     // Handles any exception from the database stage
 *     return new OrderResult(null, "DATABASE_ERROR");
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see FallBack
 * @see Exception
 * @see Catch
 * @see com.garganttua.core.runtime.IRuntimeStepOnException
 */
@Indexed
@Native
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnException {

    /**
     * The exception type to handle.
     *
     * <p>
     * The fallback will be triggered when an exception assignable to this type occurs.
     * This supports polymorphic matching, so specifying a superclass will match all
     * subclass exceptions.
     * </p>
     *
     * @return the exception class to handle
     */
    Class<? extends Throwable> exception();

    /**
     * The source step name to filter exceptions by.
     *
     * <p>
     * When specified, the fallback will only be triggered for exceptions from the step
     * with this name. An empty string (default) acts as a wildcard, matching exceptions
     * from any step.
     * </p>
     *
     * @return the step name to match, or empty string for any step
     */
    String fromStep() default "";

}
