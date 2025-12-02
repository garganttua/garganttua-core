package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Marks a field containing initial variable definitions for a runtime.
 *
 * <p>
 * The Variables annotation is applied to fields in {@code @RuntimeDefinition} classes to
 * specify initial variables that should be available in the runtime context before execution
 * begins. This is useful for providing configuration values, constants, or shared data that
 * all steps can access.
 * </p>
 *
 * <p>
 * The annotated field is typically a Map or similar collection containing variable names
 * as keys and their initial values. These variables are set in the context before the first
 * step executes and can be accessed or modified by any step.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderRuntime {
 *
 *     @Variables
 *     private Map<String, Object> initialVariables = Map.of(
 *         "processingStartedAt", Instant.now(),
 *         "maxRetries", 3,
 *         "timeout", Duration.ofSeconds(30),
 *         "environment", "production"
 *     );
 *
 *     @Operation
 *     public void processOrder(
 *             @Input Order order,
 *             @Variable(name = "maxRetries") Integer retries,
 *             @Variable(name = "timeout") Duration timeout) {
 *
 *         // Initial variables are available for injection
 *         logger.info("Processing with max retries: {}, timeout: {}", retries, timeout);
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example - Dynamic Variables</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class ConfigurableRuntime {
 *
 *     @Variables
 *     private Map<String, Object> variables;
 *
 *     public ConfigurableRuntime(Map<String, Object> config) {
 *         this.variables = new HashMap<>(config);
 *         // Add computed values
 *         this.variables.put("runtimeId", UUID.randomUUID().toString());
 *         this.variables.put("createdAt", Instant.now());
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see RuntimeDefinition
 * @see Variable
 * @see com.garganttua.core.runtime.IRuntimeContext#setVariable(String, Object)
 * @see com.garganttua.core.runtime.IRuntimeContext#getVariable(String, Class)
 */
@Native
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Variables {

}
