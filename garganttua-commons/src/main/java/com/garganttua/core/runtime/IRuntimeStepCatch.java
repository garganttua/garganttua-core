package com.garganttua.core.runtime;

/**
 * Defines exception catching behavior for a runtime step.
 *
 * <p>
 * IRuntimeStepCatch specifies which exception type should be caught by a step
 * and what result code should be set when that exception is caught. This allows
 * for fine-grained exception handling within runtime workflows.
 * </p>
 *
 * <p>
 * Catch configurations are typically defined using the {@code @Catch} annotation
 * or programmatically via the DSL. When a step throws an exception matching the
 * configured type, the exception is caught, the specified code is set in the context,
 * and execution continues (unless the step is configured to abort).
 * </p>
 *
 * <h2>Usage Example - Annotation-Based</h2>
 * <pre>{@code
 * @Operation
 * @Catch(exception = IllegalArgumentException.class, code = 400)
 * @Catch(exception = IOException.class, code = 500)
 * public void validateOrder(@Input Order order) {
 *     if (order.getAmount() <= 0) {
 *         throw new IllegalArgumentException("Invalid amount");
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example - DSL-Based</h2>
 * <pre>{@code
 * stepBuilder
 *     .method()
 *         .name("validate")
 *         .katch(IllegalArgumentException.class).code(400).end()
 *         .katch(IOException.class).code(500).end()
 *         .end();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.annotations.Catch
 * @see com.garganttua.core.runtime.dsl.IRuntimeStepCatchBuilder
 * @see IRuntimeStepMethodBinder
 */
public interface IRuntimeStepCatch {

    /**
     * Returns the exception type that should be caught.
     *
     * <p>
     * When a step throws an exception that is assignable to this type,
     * the exception will be caught and handled according to this catch configuration.
     * </p>
     *
     * @return the exception class to catch
     */
    Class<? extends Throwable> exception();

    /**
     * Returns the result code to set when this exception is caught.
     *
     * <p>
     * The code is set in the runtime context and will be available in the
     * final {@link IRuntimeResult}. By convention, non-zero codes indicate errors.
     * </p>
     *
     * @return the result code to set when this exception is caught
     * @see IRuntimeContext#setCode(int)
     * @see IRuntimeResult#code()
     */
    Integer code();

}
