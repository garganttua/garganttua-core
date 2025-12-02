package com.garganttua.core.injection;

import com.garganttua.core.CoreException;

/**
 * Exception thrown when dependency injection operations fail.
 *
 * <p>
 * {@code DiException} is the primary exception type for all dependency injection-related errors
 * in the Garganttua framework. This includes failures during context initialization, bean resolution,
 * dependency injection, bean instantiation, lifecycle management, and configuration errors. All
 * instances of this exception are automatically tagged with {@link CoreException#INJECTION_ERROR}
 * error code for consistent error handling and reporting.
 * </p>
 *
 * <h2>Common Causes</h2>
 * <ul>
 * <li>Missing required dependencies (bean not found in context)</li>
 * <li>Circular dependencies between beans</li>
 * <li>Bean instantiation failures (constructor exceptions)</li>
 * <li>Invalid bean configuration (missing annotations, invalid strategy)</li>
 * <li>Property resolution failures (missing or invalid properties)</li>
 * <li>Post-construct method failures</li>
 * <li>Context lifecycle errors</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     IDiContext context = DiContextBuilder.create()
 *         .withPackage("com.myapp")
 *         .build();
 *     context.initialize();
 * } catch (DiException e) {
 *     logger.error("Failed to initialize DI context: " + e.getMessage(), e);
 *     // Handle initialization failure
 * }
 *
 * // Custom exception with cause
 * try {
 *     MyBean bean = beanFactory.supply(context);
 * } catch (Exception e) {
 *     throw new DiException("Failed to instantiate MyBean", e);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see CoreException
 * @see IDiContext
 * @see IBeanFactory
 */
public class DiException extends CoreException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new dependency injection exception with a message and cause.
     *
     * @param message the detailed error message
     * @param cause the underlying exception that caused this error
     */
    public DiException(String message, Exception cause) {
        super(CoreException.INJECTION_ERROR, message, cause);
    }

    /**
     * Constructs a new dependency injection exception with a cause.
     *
     * <p>
     * The error message will be derived from the cause's message.
     * </p>
     *
     * @param cause the underlying exception that caused this error
     */
     public DiException(Exception cause) {
        super(CoreException.INJECTION_ERROR, cause);
    }

    /**
     * Constructs a new dependency injection exception with a message.
     *
     * @param msg the detailed error message
     */
    public DiException(String msg) {
        super(CoreException.INJECTION_ERROR, msg);
    }

}
