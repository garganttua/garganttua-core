package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a method to store its return value as a variable, or a parameter to inject a variable value.
 *
 * <p>
 * When applied to a <b>method</b>, Variable causes the method's return value to be stored
 * in the runtime context as a named variable. This enables inter-step communication by allowing
 * one step to store data that subsequent steps can retrieve.
 * </p>
 *
 * <p>
 * When applied to a <b>parameter</b>, Variable injects the value of the named variable from
 * the runtime context into the method parameter. If the variable does not exist or has the
 * wrong type, the parameter will be null.
 * </p>
 *
 * <h2>Usage Example - Storing Variables</h2>
 * <pre>{@code
 * @Operation
 * @Variable(name = "validatedAt")
 * public Instant recordValidation(@Input Order order) {
 *     // Return value is stored as variable "validatedAt"
 *     return Instant.now();
 * }
 *
 * @Operation
 * @Variable(name = "orderId")
 * public String extractOrderId(@Input Order order) {
 *     // Return value is stored as variable "orderId"
 *     return order.getId();
 * }
 * }</pre>
 *
 * <h2>Usage Example - Reading Variables</h2>
 * <pre>{@code
 * @Operation
 * public void processWithTimestamp(
 *         @Input Order order,
 *         @Variable(name = "validatedAt") Instant validationTime,
 *         @Variable(name = "orderId") String orderId) {
 *
 *     // Variables from previous steps are injected
 *     Duration elapsed = Duration.between(validationTime, Instant.now());
 *     logger.info("Processing order {} after {} validation", orderId, elapsed);
 * }
 * }</pre>
 *
 * <h2>Usage Example - Combined with Context</h2>
 * <pre>{@code
 * @Operation
 * @Variable(name = "result")
 * public OrderResult process(@Input Order order, @Context IRuntimeContext<Order, OrderResult> ctx) {
 *     OrderResult result = new OrderResult(order.getId(), "PROCESSED");
 *
 *     // Both store in variable AND set as output
 *     ctx.setOutput(result);
 *
 *     return result;  // Also stored in variable "result"
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see Output
 * @see Variables
 * @see com.garganttua.core.runtime.IRuntimeContext#setVariable(String, Object)
 * @see com.garganttua.core.runtime.IRuntimeContext#getVariable(String, Class)
 */
@Indexed
@Native
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {

    /**
     * The name of the variable.
     *
     * <p>
     * When used on a method, this is the name under which the return value will be stored.
     * When used on a parameter, this is the name of the variable to retrieve from the context.
     * </p>
     *
     * <p>
     * Variable names should be descriptive and unique within the runtime to avoid collisions.
     * </p>
     *
     * @return the variable name
     */
    String name();

}
