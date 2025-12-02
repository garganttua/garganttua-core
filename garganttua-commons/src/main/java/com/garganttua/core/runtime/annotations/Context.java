package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a parameter to inject the runtime context.
 *
 * <p>
 * The Context annotation is applied to method parameters to inject the current
 * {@link com.garganttua.core.runtime.IRuntimeContext} instance. This provides access
 * to the runtime's shared state, variables, input, output, and exception information.
 * </p>
 *
 * <p>
 * The annotated parameter must be of type {@code IRuntimeContext<InputType, OutputType>}
 * where InputType and OutputType match the runtime's type parameters.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Operation
 * public void processOrder(
 *         @Input Order order,
 *         @Context IRuntimeContext<Order, OrderResult> context) {
 *
 *     // Access input
 *     Optional<Order> input = context.getInput();
 *
 *     // Store intermediate results
 *     context.setVariable("processedAt", Instant.now());
 *     context.setVariable("orderId", order.getId());
 *
 *     // Set output
 *     context.setOutput(new OrderResult(order.getId(), "PROCESSED"));
 *
 *     // Set result code
 *     context.setCode(200);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.IRuntimeContext
 * @see Input
 * @see Output
 * @see Variable
 */
@Native
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Context {

}
