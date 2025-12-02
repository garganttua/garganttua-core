package com.garganttua.core.runtime;

/**
 * Defines exception handling criteria for triggering fallback methods.
 *
 * <p>
 * IRuntimeStepOnException specifies the conditions under which a fallback method
 * should be invoked based on exceptions that occurred in other steps. This enables
 * cross-step exception handling, where a fallback in one step can respond to
 * exceptions from other steps in the same or different stages.
 * </p>
 *
 * <p>
 * OnException configurations are typically defined using the {@code @OnException}
 * annotation or programmatically via the DSL. They support filtering by runtime name,
 * stage name, step name, and exception type.
 * </p>
 *
 * <h2>Usage Example - Annotation-Based</h2>
 * <pre>{@code
 * @FallBack
 * @OnException(
 *     exception = IllegalArgumentException.class,
 *     fromStage = "validation",
 *     fromStep = "validateAmount"
 * )
 * @Output
 * public OrderResult handleValidationError(@Exception Throwable e) {
 *     // Handle validation errors from specific step
 *     return new OrderResult(null, "VALIDATION_ERROR: " + e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Usage Example - DSL-Based</h2>
 * <pre>{@code
 * stepBuilder
 *     .fallBack()
 *         .name("handleError")
 *         .output(true)
 *         .onException(IllegalArgumentException.class)
 *             .fromStage("validation")
 *             .fromStep("validateAmount")
 *             .end()
 *         .end();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.annotations.OnException
 * @see com.garganttua.core.runtime.dsl.IRuntimeStepOnExceptionBuilder
 * @see IRuntimeStepFallbackBinder
 * @see RuntimeExceptionRecord
 */
public interface IRuntimeStepOnException {

    /**
     * Returns the runtime name to filter exceptions by.
     *
     * <p>
     * When specified, the fallback will only be triggered for exceptions from
     * the runtime with this name. An empty or null value acts as a wildcard,
     * matching exceptions from any runtime.
     * </p>
     *
     * @return the runtime name to match, or null/empty for any runtime
     */
    String runtimeName();

    /**
     * Returns the stage name to filter exceptions by.
     *
     * <p>
     * When specified, the fallback will only be triggered for exceptions from
     * steps in the stage with this name. An empty or null value acts as a wildcard,
     * matching exceptions from any stage.
     * </p>
     *
     * @return the stage name to match, or null/empty for any stage
     */
    String fromStage();

    /**
     * Returns the step name to filter exceptions by.
     *
     * <p>
     * When specified, the fallback will only be triggered for exceptions from
     * the step with this name. An empty or null value acts as a wildcard,
     * matching exceptions from any step.
     * </p>
     *
     * @return the step name to match, or null/empty for any step
     */
    String fromStep();

    /**
     * Returns the exception type to filter by.
     *
     * <p>
     * The fallback will only be triggered for exceptions that are assignable to
     * this type. This supports polymorphic matching, so specifying a superclass
     * will match all subclass exceptions.
     * </p>
     *
     * @return the exception class to match
     */
    Class<? extends Throwable> exception();

}
