/**
 * Lifecycle management framework implementation for component state management.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the lifecycle management framework.
 * It implements lifecycle state transitions with support for start/stop callbacks,
 * state validation, and lifecycle event handling.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code AbstractLifecycle} - Base abstract lifecycle implementation</li>
 * </ul>
 *
 * <h2>Lifecycle States</h2>
 * <ul>
 *   <li><b>NEW</b> - Component created but not started</li>
 *   <li><b>STARTING</b> - Component is starting</li>
 *   <li><b>STARTED</b> - Component is running</li>
 *   <li><b>STOPPING</b> - Component is stopping</li>
 *   <li><b>STOPPED</b> - Component is stopped</li>
 *   <li><b>FAILED</b> - Component failed to start or stop</li>
 * </ul>
 *
 * <h2>Usage Example: Custom Lifecycle Component</h2>
 * <pre>{@code
 * public class DatabaseService extends AbstractLifecycle {
 *
 *     private Connection connection;
 *
 *     @Override
 *     protected void doStart() {
 *         // Called during start transition
 *         connection = DriverManager.getConnection(dbUrl);
 *         connection.setAutoCommit(false);
 *     }
 *
 *     @Override
 *     protected void doStop() {
 *         // Called during stop transition
 *         if (connection != null) {
 *             connection.close();
 *             connection = null;
 *         }
 *     }
 *
 *     public void executeQuery(String sql) {
 *         ensureStarted();  // Validates lifecycle state
 *         // Execute query
 *     }
 * }
 *
 * // Usage
 * DatabaseService service = new DatabaseService();
 * service.onStart();  // Transitions to STARTED
 * service.executeQuery("SELECT * FROM users");
 * service.onStop();   // Transitions to STOPPED
 * }</pre>
 *
 * <h2>Usage Example: Lifecycle State Checking</h2>
 * <pre>{@code
 * public class CacheService extends AbstractLifecycle {
 *
 *     private Map<String, Object> cache;
 *
 *     @Override
 *     protected void doStart() {
 *         cache = new ConcurrentHashMap<>();
 *     }
 *
 *     @Override
 *     protected void doStop() {
 *         cache.clear();
 *         cache = null;
 *     }
 *
 *     public void put(String key, Object value) {
 *         if (!isStarted()) {
 *             throw new IllegalStateException("Service not started");
 *         }
 *         cache.put(key, value);
 *     }
 *
 *     public Object get(String key) {
 *         ensureStarted();  // Throws if not started
 *         return cache.get(key);
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example: Error Handling</h2>
 * <pre>{@code
 * public class MessageProcessor extends AbstractLifecycle {
 *
 *     @Override
 *     protected void doStart() {
 *         try {
 *             initializeConnections();
 *             startWorkerThreads();
 *         } catch (Exception e) {
 *             // Lifecycle automatically transitions to FAILED
 *             throw new RuntimeException("Failed to start processor", e);
 *         }
 *     }
 *
 *     @Override
 *     protected void doStop() {
 *         try {
 *             stopWorkerThreads();
 *             closeConnections();
 *         } catch (Exception e) {
 *             // Log error but allow stop to complete
 *             logger.error("Error during stop", e);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>State Transitions</h2>
 * <pre>
 * NEW -> STARTING -> STARTED
 *  |                    |
 *  |                    v
 *  |                STOPPING -> STOPPED
 *  |                    |
 *  +-----> FAILED <-----+
 * </pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>State machine implementation</li>
 *   <li>Automatic state transitions</li>
 *   <li>Start/stop callback hooks</li>
 *   <li>State validation</li>
 *   <li>Error handling and FAILED state</li>
 *   <li>Thread-safe state management</li>
 *   <li>Idempotent start/stop operations</li>
 *   <li>State query methods (isStarted, isStopped, etc.)</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * This lifecycle implementation is used by:
 * </p>
 * <ul>
 *   <li>Dependency injection context (start/stop)</li>
 *   <li>Runtime execution engine</li>
 *   <li>Service components</li>
 *   <li>Resource managers</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.lifecycle
 */
package com.garganttua.core.lifecycle;
