package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a method to produce the final runtime output.
 *
 * <p>
 * The Output annotation is applied to {@code @Operation} or {@code @FallBack} methods to
 * indicate that their return value should be set as the final output of the runtime execution.
 * This output will be available in the {@link com.garganttua.core.runtime.IRuntimeResult}.
 * </p>
 *
 * <p>
 * Only one method should typically be marked with {@code @Output} in a runtime, although
 * multiple methods can set output (the last one wins). The method's return type must match
 * the runtime's output type as specified in {@link RuntimeDefinition#output()}.
 * </p>
 *
 * <h2>Usage Example - Operation with Output</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderRuntime {
 *
 *     @Operation
 *     public void validateOrder(@Input Order order) {
 *         // Validation logic
 *     }
 *
 *     @Operation
 *     @Output  // This method's return value becomes the runtime output
 *     public OrderResult processOrder(@Input Order order) {
 *         return new OrderResult(order.getId(), "PROCESSED");
 *     }
 * }
 *
 * // Execute runtime
 * Optional<IRuntimeResult<Order, OrderResult>> result = runtime.execute(order);
 * OrderResult output = result.get().output();  // Gets the OrderResult from processOrder()
 * }</pre>
 *
 * <h2>Usage Example - Fallback with Output</h2>
 * <pre>{@code
 * @Operation
 * @Output
 * public OrderResult processOrder(@Input Order order) {
 *     // May throw exception
 *     return expensiveOperation(order);
 * }
 *
 * @FallBack
 * @Output  // Fallback can also provide output
 * public OrderResult handleError(@Exception Throwable e, @Input Order order) {
 *     // Return safe default output on error
 *     return new OrderResult(order.getId(), "FAILED: " + e.getMessage());
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see FallBack
 * @see RuntimeDefinition
 * @see Variable
 * @see com.garganttua.core.runtime.IRuntimeContext#setOutput(Object)
 * @see com.garganttua.core.runtime.IRuntimeResult#output()
 */
@Indexed
@Native
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Output {

}
