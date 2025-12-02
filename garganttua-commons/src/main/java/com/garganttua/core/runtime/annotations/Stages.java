package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a field containing stage definitions for a runtime.
 *
 * <p>
 * The Stages annotation is applied to fields in {@code @RuntimeDefinition} classes to
 * specify an explicit list or collection of stages that make up the runtime workflow.
 * This provides a way to organize runtime operations into logical groups.
 * </p>
 *
 * <p>
 * The annotated field can be of various collection types (List, Array, etc.) containing
 * stage objects. Each stage object should contain {@code @Operation} methods representing
 * the steps within that stage.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderRuntime {
 *
 *     @Stages
 *     private List<Object> stages = Arrays.asList(
 *         new ValidationStage(),
 *         new ProcessingStage(),
 *         new PersistenceStage()
 *     );
 * }
 *
 * // ValidationStage class
 * @Step
 * public class ValidationStage {
 *     @Operation
 *     public void validateOrder(@Input Order order) {
 *         // Validation logic
 *     }
 * }
 *
 * // ProcessingStage class
 * @Step
 * public class ProcessingStage {
 *     @Operation
 *     @Output
 *     public OrderResult process(@Input Order order) {
 *         return new OrderResult(order.getId(), "PROCESSED");
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see RuntimeDefinition
 * @see Step
 * @see Operation
 * @see com.garganttua.core.runtime.IRuntimeStage
 */
@Native
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Stages {

}
