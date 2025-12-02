package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.runtime.IRuntime;

/**
 * Marks a method to catch and handle specific exception types during runtime execution.
 *
 * <p>
 * The Catch annotation is applied to {@code @Operation} methods to specify which exceptions
 * should be caught and handled gracefully. When the annotated method throws an exception
 * matching the specified type, it is caught, the configured error code is set in the context,
 * and execution continues (unless abort is configured).
 * </p>
 *
 * <p>
 * Multiple Catch annotations can be applied to a single method to handle different exception types
 * with different error codes. This enables fine-grained exception handling within runtime workflows.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Operation
 * @Catch(exception = IllegalArgumentException.class, code = 400)
 * @Catch(exception = IOException.class, code = 500)
 * public void processOrder(@Input Order order) throws IOException {
 *     // If IllegalArgumentException is thrown, code 400 is set
 *     // If IOException is thrown, code 500 is set
 *     // Both are caught and execution continues
 *     if (order.getAmount() <= 0) {
 *         throw new IllegalArgumentException("Invalid amount");
 *     }
 *     // ... may throw IOException
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see FallBack
 * @see Code
 * @see com.garganttua.core.runtime.IRuntimeStepCatch
 */
@Native
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Catch {

    /**
     * The exception type to catch.
     *
     * <p>
     * When the annotated method throws an exception that is assignable to this type,
     * the exception will be caught and the specified code will be set. This supports
     * polymorphic matching, so specifying a superclass will catch all subclass exceptions.
     * </p>
     *
     * @return the exception class to catch
     */
    Class<? extends Throwable> exception();

    /**
     * The error code to set when this exception is caught.
     *
     * <p>
     * This code will be set in the runtime context and included in the final result.
     * By convention, non-zero codes indicate errors. The default value is
     * {@link IRuntime#GENERIC_RUNTIME_ERROR_CODE} (50).
     * </p>
     *
     * @return the error code to set, defaults to 50
     * @see com.garganttua.core.runtime.IRuntimeContext#setCode(int)
     */
    int code() default IRuntime.GENERIC_RUNTIME_ERROR_CODE;

}
