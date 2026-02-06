package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a field containing a condition that determines whether a step should execute.
 *
 * <p>
 * The Condition annotation is applied to fields that hold condition objects used to
 * determine if a runtime step should be executed. Conditions provide dynamic control
 * flow within runtime workflows, allowing steps to be conditionally skipped based on
 * runtime state or business logic.
 * </p>
 *
 * <p>
 * The annotated field typically holds a condition object that implements the condition
 * evaluation logic. Before a step executes, its condition (if present) is evaluated.
 * If the condition evaluates to false, the step is skipped.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Step
 * public class ConditionalProcessingStep {
 *
 *     @Condition
 *     private ICondition shouldProcess = (context) -> {
 *         // Only process if amount is greater than 100
 *         Order order = context.getInput().orElse(null);
 *         return order != null && order.getAmount() > 100;
 *     };
 *
 *     @Operation
 *     public void processLargeOrder(@Input Order order) {
 *         // This only executes if shouldProcess condition returns true
 *         logger.info("Processing large order: {}", order.getId());
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example - Context-Based Condition</h2>
 * <pre>{@code
 * @Step
 * public class RetryStep {
 *
 *     @Condition
 *     private ICondition hasRetriesLeft = (context) -> {
 *         Integer retries = context.getVariable("retriesRemaining", Integer.class).orElse(0);
 *         return retries > 0;
 *     };
 *
 *     @Operation
 *     public void retryOperation(
 *             @Input Order order,
 *             @Variable(name = "retriesRemaining") Integer retries) {
 *         // Only executes if retriesRemaining > 0
 *         // ... retry logic
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see Step
 * @see com.garganttua.core.condition.ICondition
 */
@Indexed
@Native
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {


}
