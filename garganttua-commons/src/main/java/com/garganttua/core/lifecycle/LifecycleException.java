package com.garganttua.core.lifecycle;

import com.garganttua.core.CoreException;

/**
 * Exception thrown when an error occurs during lifecycle operations.
 * <p>
 * This exception indicates failures in lifecycle management, such as:
 * <ul>
 *   <li>Component initialization failures</li>
 *   <li>Startup errors</li>
 *   <li>Shutdown problems</li>
 *   <li>Resource allocation or cleanup failures</li>
 *   <li>Configuration reload errors</li>
 * </ul>
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * public class ServiceManager implements ILifecycle {
 *
 *     {@literal @}Override
 *     public ILifecycle onStart() throws LifecycleException {
 *         try {
 *             // Attempt to start service
 *             startService();
 *         } catch (Exception e) {
 *             throw new LifecycleException("Failed to start service: " + e.getMessage());
 *         }
 *         return this;
 *     }
 *
 *     {@literal @}Override
 *     public ILifecycle onStop() throws LifecycleException {
 *         try {
 *             // Attempt to stop service
 *             stopService();
 *         } catch (IOException e) {
 *             throw new LifecycleException(e);
 *         }
 *         return this;
 *     }
 * }
 *
 * // Handle lifecycle exceptions
 * ILifecycle service = new ServiceManager();
 * try {
 *     service.onInit().onStart();
 * } catch (LifecycleException e) {
 *     System.err.println("Lifecycle operation failed: " + e.getMessage());
 *     // Perform cleanup or recovery
 * }
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public class LifecycleException extends CoreException {

    /**
     * Creates a new LifecycleException with a descriptive message.
     *
     * @param string the error message describing the lifecycle failure
     */
    public LifecycleException(String string) {
        super(CoreException.LIFECYCLE_ERROR, string);
    }

    /**
     * Creates a new LifecycleException wrapping an existing exception.
     *
     * @param e the underlying exception that caused the lifecycle failure
     */
    public LifecycleException(Exception e) {
        super(CoreException.LIFECYCLE_ERROR, e);
    }

}
