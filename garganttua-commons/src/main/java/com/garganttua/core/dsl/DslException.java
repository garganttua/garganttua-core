package com.garganttua.core.dsl;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.CoreException;

/**
 * Exception thrown when errors occur during DSL builder operations.
 *
 * <p>
 * {@code DslException} is the primary exception type for all builder-related errors
 * in the Garganttua DSL framework. It is thrown when builder validation fails,
 * configuration is invalid, automatic detection encounters problems, or object
 * construction cannot be completed successfully.
 * </p>
 *
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Missing required configuration parameters</li>
 *   <li>Invalid configuration values</li>
 *   <li>Automatic detection failures</li>
 *   <li>Builder state inconsistencies</li>
 *   <li>Object construction errors</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     Config config = new ConfigBuilder()
 *         .autoDetect(true)
 *         .build();
 * } catch (DslException e) {
 *     // Handle builder configuration errors
 *     logger.error("Failed to build configuration: " + e.getMessage(), e);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see CoreException
 * @see IBuilder
 */
public class DslException extends CoreException {
    private static final Logger log = Logger.getLogger(DslException.class);

    private static final long serialVersionUID = 1L;

    /**
     * Optional structured context attached when this exception was thrown
     * from a builder-dependency stage. Null for plain failures.
     */
    private com.garganttua.core.dsl.dependency.StageFailureContext stageFailure;

    /**
     * Attach a {@link com.garganttua.core.dsl.dependency.StageFailureContext}
     * to this exception for downstream diagnostics consumers (banner,
     * tooling, observability listeners). Returns {@code this} for fluent
     * chaining at the throw site.
     */
    public DslException withStageFailure(com.garganttua.core.dsl.dependency.StageFailureContext context) {
        this.stageFailure = context;
        return this;
    }

    /**
     * @return the attached stage-failure context if any, otherwise empty.
     */
    public java.util.Optional<com.garganttua.core.dsl.dependency.StageFailureContext> stageFailure() {
        return java.util.Optional.ofNullable(this.stageFailure);
    }

    /**
     * Constructs a new DSL exception with the specified message.
     *
     * @param message the detailed error message describing what went wrong
     */
    public DslException(String message) {
        super(CoreException.DSL_ERROR, message);
        log.trace("Exiting DslException constructor");
    }

    /**
     * Constructs a new DSL exception with the specified message and cause.
     *
     * @param message the detailed error message describing what went wrong
     * @param cause the underlying exception that caused this error
     */
    public DslException(String message, Exception cause) {
        super(CoreException.DSL_ERROR, message, cause);
        log.trace("Exiting DslException constructor");
    }

    /**
     * Constructs a new DSL exception wrapping another exception.
     *
     * @param e the exception to wrap
     */
    public DslException(Exception e) {
        super(e);
        log.trace("Exiting DslException constructor");
    }

}
