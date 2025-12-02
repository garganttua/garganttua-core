package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.execution.IExecutor;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

/**
 * Binds a method to a runtime step for execution.
 *
 * <p>
 * IRuntimeStepMethodBinder extends {@link IContextualMethodBinder} and {@link IExecutor}
 * to provide runtime-specific method binding capabilities. It handles method invocation,
 * parameter injection, result handling, and integration with the runtime context.
 * </p>
 *
 * <p>
 * Method binders are created either from {@code @Operation} annotated methods or
 * programmatically using the DSL. They manage the binding between runtime parameters
 * (Input, Context, Variables, Exception, etc.) and method parameters.
 * </p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Invoke the bound method with injected parameters</li>
 *   <li>Handle method return values (output or variable storage)</li>
 *   <li>Set result codes in the context</li>
 *   <li>Support nullable return values</li>
 * </ul>
 *
 * @param <ExecutionReturned> the return type of the bound method
 * @param <OwnerContextType> the runtime context type
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime.annotations.Operation
 * @see com.garganttua.core.runtime.dsl.IRuntimeStepMethodBuilder
 * @see com.garganttua.core.reflection.binders.IContextualMethodBinder
 */
public interface IRuntimeStepMethodBinder<ExecutionReturned, OwnerContextType, InputType,OutputType> extends IContextualMethodBinder<ExecutionReturned, OwnerContextType>, IExecutor<OwnerContextType> {

    /**
     * Indicates whether this method produces the runtime output.
     *
     * <p>
     * When true, the method's return value will be set as the final output
     * in the runtime context via {@link IRuntimeContext#setOutput(Object)}.
     * </p>
     *
     * @return true if this method produces output, false otherwise
     * @see com.garganttua.core.runtime.annotations.Output
     */
    boolean isOutput();

    /**
     * Returns the variable name where this method's return value should be stored.
     *
     * <p>
     * If present, the method's return value will be stored as a named variable
     * in the runtime context instead of being used as output.
     * </p>
     *
     * @return an Optional containing the variable name if configured, otherwise empty
     * @see com.garganttua.core.runtime.annotations.Variable
     * @see IRuntimeContext#setVariable(String, Object)
     */
    Optional<String> variable();

    /**
     * Sets the result code in the runtime context.
     *
     * <p>
     * This method is called during execution to set the configured result code
     * for this method in the runtime context.
     * </p>
     *
     * @param c the runtime context in which to set the code
     * @see com.garganttua.core.runtime.annotations.Code
     * @see IRuntimeContext#setCode(int)
     */
    void setCode(IRuntimeContext<?,?> c);

    /**
     * Indicates whether null return values are acceptable.
     *
     * <p>
     * When true, null return values from the method are allowed and will not
     * cause validation errors. When false, null returns may trigger errors
     * depending on the runtime configuration.
     * </p>
     *
     * @return true if null return values are allowed, false otherwise
     */
    boolean nullable();

}
