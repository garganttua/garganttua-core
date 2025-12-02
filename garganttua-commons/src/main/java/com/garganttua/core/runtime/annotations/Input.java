package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a parameter to inject the runtime input.
 *
 * <p>
 * The Input annotation is applied to method parameters to inject the original input object
 * provided to the runtime execution. This allows all steps within a runtime to access the
 * same input data without explicit passing between methods.
 * </p>
 *
 * <p>
 * The annotated parameter must be of the runtime's input type as specified in
 * {@link RuntimeDefinition#input()} or the DSL builder.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderRuntime {
 *
 *     @Operation
 *     public void validateOrder(@Input Order order) {
 *         if (order.getAmount() <= 0) {
 *             throw new IllegalArgumentException("Invalid amount");
 *         }
 *     }
 *
 *     @Operation
 *     @Output
 *     public OrderResult processOrder(@Input Order order) {
 *         // Process the same order input
 *         return new OrderResult(order.getId(), "PROCESSED");
 *     }
 *
 *     @FallBack
 *     @Output
 *     public OrderResult handleError(@Exception Throwable e, @Input Order order) {
 *         // Fallback can also access input
 *         return new OrderResult(order.getId(), "FAILED");
 *     }
 * }
 *
 * // Execute runtime with input
 * Order order = new Order("ORD-123", 100.0);
 * runtime.execute(order);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see RuntimeDefinition
 * @see Output
 * @see Context
 * @see com.garganttua.core.runtime.IRuntimeContext#getInput()
 */
@Native
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Input {

}
