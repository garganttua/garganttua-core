package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.execution.IFallBackExecutor;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

/**
 * Binds a fallback method to a runtime step for error recovery.
 *
 * <p>
 * IRuntimeStepFallbackBinder extends {@link IContextualMethodBinder} and {@link IFallBackExecutor}
 * to provide fallback method binding capabilities. Fallback methods are invoked when a step's
 * main method throws an uncaught exception, providing a mechanism for error recovery and
 * graceful degradation.
 * </p>
 *
 * <p>
 * Fallback binders are created either from {@code @FallBack} annotated methods or
 * programmatically using the DSL. They have access to exception information via
 * {@code @Exception} parameters and can produce output or store variables.
 * </p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Invoke the fallback method when the main method fails</li>
 *   <li>Inject exception information into method parameters</li>
 *   <li>Handle fallback return values (output or variable storage)</li>
 *   <li>Support nullable return values</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Operation
 * public OrderResult processOrder(@Input Order order) {
 *     // May throw exception
 *     return expensiveOperation(order);
 * }
 *
 * @FallBack
 * @Output
 * public OrderResult handleProcessingError(@Exception Throwable e, @Input Order order) {
 *     // Fallback logic - return safe default
 *     return new OrderResult(order.getId(), "FAILED: " + e.getMessage());
 * }
 * }</pre>
 *
 * @param <ExecutionReturned> the return type of the fallback method
 * @param <OwnerContextType> the runtime context type
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.annotations.FallBack
 * @see com.garganttua.core.runtime.dsl.IRuntimeStepFallbackBuilder
 * @see com.garganttua.core.reflection.binders.IContextualMethodBinder
 */
public interface IRuntimeStepFallbackBinder<ExecutionReturned, OwnerContextType, InputType, OutputType> extends IContextualMethodBinder<ExecutionReturned, OwnerContextType>, IFallBackExecutor<OwnerContextType> {

    /**
     * Indicates whether this fallback method produces the runtime output.
     *
     * <p>
     * When true, the fallback method's return value will be set as the final output
     * in the runtime context via {@link IRuntimeContext#setOutput(Object)}.
     * This allows fallback methods to provide alternative output when errors occur.
     * </p>
     *
     * @return true if this fallback produces output, false otherwise
     * @see com.garganttua.core.runtime.annotations.Output
     */
    boolean isOutput();

    /**
     * Returns the variable name where this fallback method's return value should be stored.
     *
     * <p>
     * If present, the fallback method's return value will be stored as a named variable
     * in the runtime context instead of being used as output.
     * </p>
     *
     * @return an Optional containing the variable name if configured, otherwise empty
     * @see com.garganttua.core.runtime.annotations.Variable
     * @see IRuntimeContext#setVariable(String, Object)
     */
    Optional<String> variable();

    /**
     * Indicates whether null return values are acceptable.
     *
     * <p>
     * When true, null return values from the fallback method are allowed and will not
     * cause validation errors. When false, null returns may trigger errors
     * depending on the runtime configuration.
     * </p>
     *
     * @return true if null return values are allowed, false otherwise
     */
    boolean nullable();

}
