package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a class as a runtime step definition.
 *
 * <p>
 * The Step annotation is applied to classes that represent individual steps within a
 * runtime. Step classes contain {@code @Operation} methods that define the step's
 * execution logic, along with optional {@code @FallBack} methods for error handling.
 * </p>
 *
 * <p>
 * Step classes are typically used in conjunction with the {@code @Steps} annotation to
 * define the workflow sequence. Each step represents an atomic unit of work.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Runtime definition with steps
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderRuntime {
 *
 *     @Steps
 *     private List<Class<?>> steps = Arrays.asList(
 *         ValidationStep.class,
 *         ProcessingStep.class
 *     );
 * }
 *
 * // Step definition
 * @Step
 * public class ValidationStep {
 *
 *     @Operation
 *     public void validateAmount(@Input Order order) {
 *         if (order.getAmount() <= 0) {
 *             throw new IllegalArgumentException("Invalid amount");
 *         }
 *     }
 *
 *     @Operation
 *     public void validateCustomer(@Input Order order) {
 *         if (order.getCustomerId() == null) {
 *             throw new IllegalArgumentException("Missing customer");
 *         }
 *     }
 *
 *     @FallBack
 *     @Variable(name = "validationFailed")
 *     public Boolean handleValidationError(@Exception Throwable e) {
 *         return true;
 *     }
 * }
 *
 * // Another step definition
 * @Step
 * public class ProcessingStep {
 *
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
 * @see Steps
 * @see Operation
 * @see FallBack
 * @see com.garganttua.core.runtime.IRuntimeStep
 */
@Native
@Qualifier
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Step {

}
