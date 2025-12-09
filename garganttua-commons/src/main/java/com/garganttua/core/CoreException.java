package com.garganttua.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base exception class for all Garganttua Core framework exceptions.
 *
 * <p>
 * CoreException serves as the root exception type for the entire framework, providing
 * a standardized error code system and utility methods for exception handling. All
 * framework-specific exceptions should extend this class to maintain consistency
 * and enable unified error management.
 * </p>
 *
 * <h2>Error Code System</h2>
 * <p>
 * Each exception carries an integer error code that categorizes the type of error:
 * </p>
 * <ul>
 *   <li>{@link #UNKNOWN_ERROR} - Generic or unclassified errors</li>
 *   <li>{@link #SUPPLY_ERROR} - Supply chain and dependency provision errors</li>
 *   <li>{@link #RUNTIME_ERROR} - Runtime workflow execution errors</li>
 *   <li>{@link #REFLECTION_ERROR} - Reflection and introspection errors</li>
 *   <li>{@link #MAPPER_ERROR} - Object mapping and transformation errors</li>
 *   <li>{@link #LIFECYCLE_ERROR} - Component lifecycle management errors</li>
 *   <li>{@link #INJECTION_ERROR} - Dependency injection errors</li>
 *   <li>{@link #EXECUTOR_ERROR} - Asynchronous execution errors</li>
 *   <li>{@link #DSL_ERROR} - Domain-specific language parsing errors</li>
 *   <li>{@link #CONDITION_ERROR} - Condition evaluation errors</li>
 *   <li>{@link #COPY_ERROR} - Object copying and cloning errors</li>
 *   <li>{@link #NATIVE_ERROR} - Native image configuration errors</li>
 * </ul>
 *
 * <h2>Exception Utilities</h2>
 * <p>
 * The class provides static utility methods for exception handling:
 * </p>
 * <ul>
 *   <li>{@link #findFirstInException(Throwable)} - Locates the first CoreException in a cause chain</li>
 *   <li>{@link #findFirstInException(Throwable, Class)} - Locates the first exception of a specific type</li>
 *   <li>{@link #processException(Throwable)} - Processes and rethrows exceptions as CoreException</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Creating a new CoreException
 * throw new CoreException(CoreException.REFLECTION_ERROR, "Failed to access field", cause);
 *
 * // Finding CoreException in a cause chain
 * try {
 *     someOperation();
 * } catch (Exception e) {
 *     Optional<CoreException> coreEx = CoreException.findFirstInException(e);
 *     if (coreEx.isPresent()) {
 *         int errorCode = coreEx.get().getCode();
 *         // Handle based on error code
 *     }
 * }
 *
 * // Processing exceptions uniformly
 * try {
 *     riskyOperation();
 * } catch (Exception e) {
 *     CoreException.processException(e); // Logs and rethrows as CoreException
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see CoreExceptionCodes
 */
@Slf4j
public class CoreException extends RuntimeException {

    /**
     * Error code for unknown or unclassified errors.
     * <p>
     * This code is used when an error cannot be categorized into any specific
     * error type, or when wrapping exceptions from external libraries.
     * </p>
     */
    public static final int UNKNOWN_ERROR = -1;

    /**
     * Error code for supply chain and dependency provision errors.
     * <p>
     * Used when errors occur during dependency supply operations, such as
     * supplier execution failures or provision errors.
     * </p>
     */
    public static final int SUPPLY_ERROR = 1;

    /**
     * Error code for runtime workflow execution errors.
     * <p>
     * Used when errors occur during runtime workflow execution, including
     * step execution failures and runtime configuration errors.
     * </p>
     */
    public static final int RUNTIME_ERROR = 2;

    /**
     * Error code for reflection and introspection errors.
     * <p>
     * Used when errors occur during reflection operations, such as accessing
     * fields, methods, or constructors.
     * </p>
     */
    public static final int REFLECTION_ERROR = 3;

    /**
     * Error code for object mapping and transformation errors.
     * <p>
     * Used when errors occur during object mapping operations, including
     * type conversion failures and mapping configuration errors.
     * </p>
     */
    public static final int MAPPER_ERROR = 4;

    /**
     * Error code for component lifecycle management errors.
     * <p>
     * Used when errors occur during lifecycle operations such as initialization,
     * startup, shutdown, or cleanup.
     * </p>
     */
    public static final int LIFECYCLE_ERROR = 5;

    /**
     * Error code for dependency injection errors.
     * <p>
     * Used when errors occur during dependency injection operations, such as
     * bean instantiation, wiring, or context initialization.
     * </p>
     */
    public static final int INJECTION_ERROR = 6;

    /**
     * Error code for asynchronous execution errors.
     * <p>
     * Used when errors occur during asynchronous task execution, including
     * thread pool errors and execution failures.
     * </p>
     */
    public static final int EXECUTOR_ERROR = 7;

    /**
     * Error code for domain-specific language parsing errors.
     * <p>
     * Used when errors occur during DSL parsing, building, or execution.
     * </p>
     */
    public static final int DSL_ERROR = 8;

    /**
     * Error code for condition evaluation errors.
     * <p>
     * Used when errors occur during condition evaluation or expression
     * processing.
     * </p>
     */
    public static final int CONDITION_ERROR = 9;

    /**
     * Error code for object copying and cloning errors.
     * <p>
     * Used when errors occur during object copying, cloning, or deep copy
     * operations.
     * </p>
     */
    public static final int COPY_ERROR = 10;

    /**
     * Error code for native image configuration errors.
     * <p>
     * Used when errors occur during GraalVM native image configuration,
     * including reflection, resource, and JNI configuration errors.
     * </p>
     */
    public static final int NATIVE_ERROR = 10;


    public static final int MUTEX_ERROR = 11;

    private static final long serialVersionUID = 7855765591949705798L;

    public static final int EXPRESSION_ERROR = 12;

    /**
     * The error code categorizing this exception.
     * <p>
     * This field stores one of the predefined error codes (e.g., {@link #RUNTIME_ERROR},
     * {@link #INJECTION_ERROR}) to identify the type of error that occurred. The code
     * can be accessed via the generated getCode() method (generated by Lombok @Getter).
     * </p>
     */
    @Getter
    protected int code = CoreException.UNKNOWN_ERROR;

    /**
     * Constructs a new CoreException with the specified error code and message.
     *
     * @param code the error code categorizing this exception
     * @param message the detail message explaining the error
     */
    protected CoreException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Constructs a new CoreException with the specified error code, message, and cause.
     *
     * @param code the error code categorizing this exception
     * @param message the detail message explaining the error
     * @param exception the cause of this exception
     */
    protected CoreException(int code, String message, Throwable exception) {
        super(message, exception);
        this.code = code;
    }

    /**
     * Constructs a new CoreException with the specified error code and cause.
     * <p>
     * The detail message is taken from the cause's message.
     * </p>
     *
     * @param code the error code categorizing this exception
     * @param exception the cause of this exception
     */
    protected CoreException(int code, Throwable exception) {
        super(exception.getMessage(), exception);
        this.code = code;
    }

    /**
     * Constructs a new CoreException wrapping another exception.
     * <p>
     * If the wrapped exception is also a CoreException, its error code is preserved.
     * Otherwise, the error code is set to {@link #UNKNOWN_ERROR}.
     * </p>
     *
     * @param exception the exception to wrap
     */
    protected CoreException(Throwable exception) {
        super(exception.getMessage(), exception);
        if (CoreException.class.isAssignableFrom(exception.getClass())) {
            this.code = ((CoreException) exception).getCode();
        } else {
            this.code = CoreException.UNKNOWN_ERROR;
        }
    }

    /**
     * Searches for the first CoreException in the exception's cause chain.
     * <p>
     * This method traverses the cause chain of the given exception, looking for
     * the first occurrence of a CoreException. This is useful for extracting
     * framework-specific exception information from wrapped exceptions.
     * </p>
     *
     * @param exception the exception whose cause chain to search
     * @return an Optional containing the first CoreException found, or empty if none found
     */
    public static Optional<CoreException> findFirstInException(Throwable exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (CoreException.class.isAssignableFrom(cause.getClass())) {
                return Optional.of((CoreException) cause);
            }
            cause = cause.getCause();
        }
        return Optional.empty();
    }

    /**
     * Processes an exception by logging it and rethrowing as CoreException.
     * <p>
     * This method provides uniform exception handling by:
     * </p>
     * <ul>
     *   <li>Logging the exception at WARN level</li>
     *   <li>Searching for an existing CoreException in the cause chain</li>
     *   <li>Rethrowing the found CoreException, or wrapping the exception as a new
     *       CoreException with {@link #UNKNOWN_ERROR} code if no CoreException is found</li>
     * </ul>
     *
     * @param e the exception to process
     * @throws CoreException always throws either the found CoreException or a new wrapped one
     */
    public static void processException(Throwable e) throws CoreException {
        log.atWarn().log("Error ", e);
        Optional<CoreException> coreException = CoreException.findFirstInException(e);
        if (coreException.isPresent()) {
            throw coreException.get();
        } else {
            throw new CoreException(CoreException.UNKNOWN_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Searches for the first exception of a specific type in the exception's cause chain.
     * <p>
     * This generic method traverses the cause chain of the given exception, looking for
     * the first occurrence of an exception of the specified type. It handles
     * {@link InvocationTargetException} specially by unwrapping the target exception.
     * </p>
     *
     * @param <E> the type of exception to search for
     * @param exception the exception whose cause chain to search
     * @param type the class of the exception type to find
     * @return an Optional containing the first exception of the specified type, or empty if none found
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Optional<E> findFirstInException(Throwable exception,
            Class<E> type) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause.getClass().isAssignableFrom(type)) {
                return Optional.of((E) cause);
            }
            if (cause instanceof InvocationTargetException inv) {
                cause = inv.getTargetException();
            } else {
                cause = cause.getCause();
            }
        }
        return Optional.empty();

    }
}
