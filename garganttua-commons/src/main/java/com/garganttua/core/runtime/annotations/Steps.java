package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a field that declares the ordered list of step classes composing a runtime.
 *
 * <p>
 * The Steps annotation is applied to a field in a {@code @RuntimeDefinition} class that holds
 * the collection of {@code @Step}-annotated classes making up the workflow. The framework reads
 * this field to determine the sequence in which steps are executed.
 * </p>
 *
 * <p>
 * The annotated field is typically a {@code List<Class<?>>} of step classes, in execution order.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderRuntime {
 *
 *     @Steps
 *     private List<Class<?>> steps = Arrays.asList(
 *         ValidationStep.class,
 *         ProcessingStep.class
 *     );
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see RuntimeDefinition
 * @see Step
 * @see com.garganttua.core.runtime.IRuntimeStep
 */
@Indexed
@Reflected
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Steps {

}
