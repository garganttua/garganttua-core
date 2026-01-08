package com.garganttua.core.lifecycle;

/**
 * Defines the lifecycle management interface for components.
 * <p>
 * This interface provides a standard set of lifecycle methods that can be implemented
 * by components to manage their initialization, startup, shutdown, and other lifecycle
 * events. All methods return the ILifecycle instance to support method chaining.
 * </p>
 *
 * <h2>Lifecycle Phases:</h2>
 * <ol>
 *   <li><b>Init</b>: Component initialization, typically called once</li>
 *   <li><b>Start</b>: Component activation and resource allocation</li>
 *   <li><b>Flush</b>: Clear caches, buffers, or temporary state</li>
 *   <li><b>Reload</b>: Reload configuration or reinitialize state</li>
 *   <li><b>Stop</b>: Component shutdown and resource cleanup</li>
 * </ol>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * public class DatabaseConnection implements ILifecycle {
 *     private Connection connection;
 *
 *     {@literal @}Override
 *     public ILifecycle onInit() throws LifecycleException {
 *         System.out.println("Initializing database connection");
 *         // Load configuration
 *         return this;
 *     }
 *
 *     {@literal @}Override
 *     public ILifecycle onStart() throws LifecycleException {
 *         try {
 *             connection = DriverManager.getConnection(url, user, password);
 *             System.out.println("Database connection started");
 *         } catch (SQLException e) {
 *             throw new LifecycleException(e);
 *         }
 *         return this;
 *     }
 *
 *     {@literal @}Override
 *     public ILifecycle onFlush() throws LifecycleException {
 *         // Clear connection pool cache
 *         System.out.println("Flushing connection pool");
 *         return this;
 *     }
 *
 *     {@literal @}Override
 *     public ILifecycle onReload() throws LifecycleException {
 *         System.out.println("Reloading database configuration");
 *         // Reload connection settings
 *         return this;
 *     }
 *
 *     {@literal @}Override
 *     public ILifecycle onStop() throws LifecycleException {
 *         try {
 *             if (connection != null {@literal &&} !connection.isClosed()) {
 *                 connection.close();
 *             }
 *             System.out.println("Database connection stopped");
 *         } catch (SQLException e) {
 *             throw new LifecycleException(e);
 *         }
 *         return this;
 *     }
 * }
 *
 * // Using the lifecycle component
 * ILifecycle db = new DatabaseConnection();
 * db.onInit()
 *   .onStart()
 *   .onFlush()
 *   .onReload()
 *   .onStop();
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
public interface ILifecycle {

    /**
     * Starts the component.
     * <p>
     * This method is called to activate the component and allocate necessary resources.
     * It is typically called after initialization and before the component is used.
     * Implementations should ensure that the component is ready to perform its
     * operations after this method completes successfully.
     * </p>
     *
     * @return this lifecycle instance for method chaining
     * @throws LifecycleException if the component cannot be started
     */
    ILifecycle onStart() throws LifecycleException;

    /**
     * Stops the component.
     * <p>
     * This method is called to gracefully shut down the component and release
     * all allocated resources. Implementations should ensure that all operations
     * are completed or safely interrupted, and all resources (connections, files,
     * threads, etc.) are properly closed.
     * </p>
     *
     * @return this lifecycle instance for method chaining
     * @throws LifecycleException if the component cannot be stopped cleanly
     */
    ILifecycle onStop() throws LifecycleException;

    /**
     * Flushes the component's state.
     * <p>
     * This method is called to clear caches, flush buffers, or reset temporary
     * state while keeping the component running. It is useful for freeing memory
     * or ensuring data consistency without fully stopping the component.
     * </p>
     *
     * @return this lifecycle instance for method chaining
     * @throws LifecycleException if the flush operation fails
     */
    ILifecycle onFlush() throws LifecycleException;

    /**
     * Initializes the component.
     * <p>
     * This method is called to perform one-time initialization of the component.
     * It should set up the initial state, load configuration, and prepare the
     * component for subsequent start operations. This is typically the first
     * lifecycle method called.
     * </p>
     *
     * @return this lifecycle instance for method chaining
     * @throws LifecycleException if initialization fails
     */
    ILifecycle onInit() throws LifecycleException;

    /**
     * Reloads the component's configuration or state.
     * <p>
     * This method is called to reload configuration, refresh state, or reinitialize
     * the component while it is running. It allows dynamic reconfiguration without
     * stopping and restarting the component.
     * </p>
     *
     * @return this lifecycle instance for method chaining
     * @throws LifecycleException if the reload operation fails
     */
    ILifecycle onReload() throws LifecycleException;

    LifecycleStatus status();

}
