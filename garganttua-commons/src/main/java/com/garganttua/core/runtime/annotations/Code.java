package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a method to set a result code or a parameter to inject the current result code.
 *
 * <p>
 * When applied to a <b>method</b>, Code specifies the result code that should be set in the
 * runtime context when the method executes successfully. This is useful for distinguishing
 * between different success scenarios or business outcomes.
 * </p>
 *
 * <p>
 * When applied to a <b>parameter</b>, Code injects the current result code from the runtime
 * context into the method parameter. This allows methods to access and react to codes set
 * by previous steps.
 * </p>
 *
 * <h2>Usage Example - Method</h2>
 * <pre>{@code
 * @Operation
 * @Code(200)  // Set code 200 on success
 * public void validateOrder(@Input Order order) {
 *     // Validation logic
 *     // Code 200 will be set if no exception is thrown
 * }
 * }</pre>
 *
 * <h2>Usage Example - Parameter</h2>
 * <pre>{@code
 * @Operation
 * public void logResult(@Code Integer previousCode, @Input Order order) {
 *     // previousCode contains the result code from previous steps
 *     logger.info("Previous result code: {}", previousCode);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see Catch
 * @see com.garganttua.core.runtime.IRuntimeContext#setCode(int)
 * @see com.garganttua.core.runtime.IRuntimeContext#getCode()
 */
@Indexed
@Native
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Code {

    /**
     * The result code value.
     *
     * <p>
     * When used on a method, this is the code to set in the context on successful execution.
     * When used on a parameter, this value is ignored (the parameter receives the current code from context).
     * </p>
     *
     * <p>
     * The default value of -1 means no code should be set (method annotation only).
     * </p>
     *
     * @return the result code value, defaults to -1 (no code)
     */
    int value() default -1;

}
