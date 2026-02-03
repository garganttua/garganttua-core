package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a class as a declarative runtime definition.
 *
 * <p>
 * The RuntimeDefinition annotation is applied to classes to define complete runtime workflows
 * using a declarative, annotation-based approach. It specifies the input and output types for
 * the runtime and marks the class for discovery and initialization by the runtime framework.
 * </p>
 *
 * <p>
 * Classes annotated with {@code @RuntimeDefinition} should contain {@code @Operation} methods
 * that define the workflow steps, and optionally {@code @FallBack} methods for error handling.
 * The framework automatically discovers these methods and builds the complete runtime execution
 * graph.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Declarative runtime definition using annotations</li>
 *   <li>Automatic method discovery and binding</li>
 *   <li>Type-safe input and output declarations</li>
 *   <li>Integration with dependency injection via {@code @Qualifier}</li>
 *   <li>Alternative to programmatic DSL approach</li>
 * </ul>
 *
 * <h2>Usage Example - Simple Runtime</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderProcessingRuntime {
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
 *         return new OrderResult(order.getId(), "PROCESSED");
 *     }
 *
 *     @FallBack
 *     @Output
 *     public OrderResult handleError(@Exception Throwable e) {
 *         return new OrderResult(null, "ERROR: " + e.getMessage());
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example - Complex Runtime with Steps</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class ComplexOrderRuntime {
 *
 *     @Steps
 *     private List<Class<?>> steps = Arrays.asList(
 *         ValidationStep.class,
 *         ProcessingStep.class,
 *         PersistenceStep.class
 *     );
 *
 *     @Variables
 *     private Map<String, Object> initialVariables = Map.of(
 *         "createdAt", Instant.now(),
 *         "version", "1.0"
 *     );
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see FallBack
 * @see Input
 * @see Output
 * @see Steps
 * @see Variables
 * @see com.garganttua.core.runtime.IRuntime
 */
@Native
@Qualifier
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeDefinition {

    /**
     * The input type for this runtime.
     *
     * <p>
     * This type will be used for all {@code @Input} parameter injections throughout
     * the runtime. It must match the type provided when calling
     * {@link com.garganttua.core.runtime.IRuntime#execute(Object)}.
     * </p>
     *
     * @return the input class type
     */
    Class<?> input();

    /**
     * The output type for this runtime.
     *
     * <p>
     * This type must match the return type of methods marked with {@code @Output}.
     * The final output will be available in the {@link com.garganttua.core.runtime.IRuntimeResult}.
     * </p>
     *
     * @return the output class type
     */
    Class<?> output();

}
