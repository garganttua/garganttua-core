package com.garganttua.core;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing constants and utilities for CoreException error codes.
 *
 * <p>
 * This class serves as a central repository for exception error code definitions
 * and related utilities. All error codes are defined as constants in the
 * {@link CoreException} class, and this class can be extended to provide
 * additional utilities for error code management.
 * </p>
 *
 * <h2>Error Code Categories</h2>
 * <p>
 * The framework uses the following error code categories:
 * </p>
 * <ul>
 *   <li><strong>-1</strong> - {@link CoreException#UNKNOWN_ERROR} - Unknown or unclassified errors</li>
 *   <li><strong>1</strong> - {@link CoreException#SUPPLY_ERROR} - Supply chain errors</li>
 *   <li><strong>2</strong> - {@link CoreException#RUNTIME_ERROR} - Runtime execution errors</li>
 *   <li><strong>3</strong> - {@link CoreException#REFLECTION_ERROR} - Reflection errors</li>
 *   <li><strong>4</strong> - {@link CoreException#MAPPER_ERROR} - Object mapping errors</li>
 *   <li><strong>5</strong> - {@link CoreException#LIFECYCLE_ERROR} - Lifecycle management errors</li>
 *   <li><strong>6</strong> - {@link CoreException#INJECTION_ERROR} - Dependency injection errors</li>
 *   <li><strong>7</strong> - {@link CoreException#EXECUTOR_ERROR} - Asynchronous execution errors</li>
 *   <li><strong>8</strong> - {@link CoreException#DSL_ERROR} - DSL parsing errors</li>
 *   <li><strong>9</strong> - {@link CoreException#CONDITION_ERROR} - Condition evaluation errors</li>
 *   <li><strong>10</strong> - {@link CoreException#COPY_ERROR} - Object copying errors</li>
 *   <li><strong>10</strong> - {@link CoreException#NATIVE_ERROR} - Native image configuration errors</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Error codes are accessed through CoreException constants
 * int errorCode = CoreException.INJECTION_ERROR;
 *
 * // Create an exception with a specific error code
 * throw new DiException("Bean not found"); // Uses CoreException.INJECTION_ERROR internally
 *
 * // Check error code from caught exception
 * try {
 *     someOperation();
 * } catch (CoreException e) {
 *     if (e.getCode() == CoreException.INJECTION_ERROR) {
 *         // Handle injection-specific errors
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see CoreException
 */
@Slf4j
public class CoreExceptionCodes {

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private CoreExceptionCodes() {
        log.atTrace().log("Entering CoreExceptionCodes constructor");
        log.atError().log("Attempt to instantiate utility class CoreExceptionCodes");
        throw new UnsupportedOperationException("CoreExceptionCodes is a utility class and cannot be instantiated");
    }

}
