package com.garganttua.core.runtime;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.CoreException;

/**
 * Exception thrown when a critical error occurs during runtime workflow execution.
 *
 * <p>
 * RuntimeException is the primary exception type for errors that occur during runtime execution.
 * It extends {@link CoreException} and can optionally carry a reference to the runtime context,
 * allowing access to execution state when handling errors.
 * </p>
 *
 * <h2>Usage Scenarios</h2>
 * <ul>
 *   <li>Wrapping exceptions from step execution</li>
 *   <li>Signaling configuration or initialization errors</li>
 *   <li>Indicating runtime execution failures</li>
 *   <li>Providing context-aware error information</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     Optional<IRuntimeResult<Order, OrderResult>> result = runtime.execute(order);
 * } catch (RuntimeException e) {
 *     // Exception provides context about where the error occurred
 *     System.err.println("Runtime execution failed: " + e.getMessage());
 *     System.err.println("Error code: " + e.getCode());
 *
 *     // Original cause is preserved
 *     Throwable cause = e.getCause();
 *     if (cause != null) {
 *         System.err.println("Caused by: " + cause.getMessage());
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IRuntime
 * @see IRuntimeContext
 * @see CoreException
 */
public class RuntimeException extends CoreException {

    /**
     * Optional reference to the runtime context where this exception occurred.
     */
    private Optional<IRuntimeContext<?, ?>> context;

    /**
     * Constructs a new runtime exception with the specified error message.
     *
     * @param message the detail message explaining the error
     */
    public RuntimeException(String message) {
        super(CoreException.RUNTIME_ERROR, message);
    }

    /**
     * Constructs a new runtime exception wrapping another exception.
     *
     * @param e the exception to wrap
     */
    public RuntimeException(Exception e) {
        super(CoreException.RUNTIME_ERROR, e);
    }

    /**
     * Constructs a new runtime exception with a message and a cause.
     *
     * @param string the detail message
     * @param e the cause of this exception
     */
    public RuntimeException(String string, Throwable e) {
        super(CoreException.RUNTIME_ERROR, string, e);
    }

    /**
     * Constructs a new runtime exception with an optional runtime context.
     *
     * <p>
     * This constructor allows attaching the runtime context to the exception,
     * providing access to execution state, variables, and other context information
     * when handling the error.
     * </p>
     *
     * @param e the exception to wrap
     * @param context an Optional containing the runtime context, must not be null
     * @throws NullPointerException if context is null
     */
    public RuntimeException(Exception e, Optional<IRuntimeContext<?,?>> context) {
        this(e);
        this.context = Objects.requireNonNull(context, "Context cannot be null");
    }

}
