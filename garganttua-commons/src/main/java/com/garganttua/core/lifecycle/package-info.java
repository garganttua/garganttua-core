/**
 * Abstract lifecycle management with thread-safe state transitions and hooks.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a robust lifecycle management framework with thread-safe state
 * transitions. Components can implement lifecycle hooks for initialization, startup,
 * shutdown, reload, and flush operations.
 * </p>
 *
 * <h2>Core Interface</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.lifecycle.ILifecycle} - Lifecycle management contract</li>
 * </ul>
 *
 * <h2>Lifecycle Phases</h2>
 * <p>
 * Components implementing {@code ILifecycle} go through these phases:
 * </p>
 * <ol>
 *   <li><b>Created</b> - Object instantiated but not initialized</li>
 *   <li><b>Initialized</b> - Configuration loaded, dependencies resolved</li>
 *   <li><b>Started</b> - Component actively running</li>
 *   <li><b>Stopped</b> - Component gracefully shut down</li>
 * </ol>
 *
 * <h2>Lifecycle Hooks</h2>
 * <ul>
 *   <li><b>onInit()</b> - Initialize configuration and resources</li>
 *   <li><b>onStart()</b> - Start component execution</li>
 *   <li><b>onStop()</b> - Stop component and release resources</li>
 *   <li><b>onReload()</b> - Reload configuration without restart</li>
 *   <li><b>onFlush()</b> - Flush buffers, caches, or pending operations</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class DatabaseService implements ILifecycle {
 *     private DataSource dataSource;
 *     private boolean started = false;
 *
 *     @Override
 *     public void onInit() throws LifecycleException {
 *         // Load database configuration
 *         String url = config.getProperty("db.url");
 *         String username = config.getProperty("db.username");
 *         String password = config.getProperty("db.password");
 *
 *         // Initialize data source
 *         this.dataSource = DataSourceBuilder.create()
 *             .url(url)
 *             .username(username)
 *             .password(password)
 *             .build();
 *
 *         System.out.println("Database service initialized");
 *     }
 *
 *     @Override
 *     public void onStart() throws LifecycleException {
 *         if (started) {
 *             throw new LifecycleException("Service already started");
 *         }
 *
 *         // Test connection
 *         try (Connection conn = dataSource.getConnection()) {
 *             System.out.println("Database connection established");
 *         } catch (SQLException e) {
 *             throw new LifecycleException("Failed to connect to database", e);
 *         }
 *
 *         started = true;
 *         System.out.println("Database service started");
 *     }
 *
 *     @Override
 *     public void onStop() throws LifecycleException {
 *         if (!started) {
 *             return;
 *         }
 *
 *         // Close all connections
 *         if (dataSource instanceof AutoCloseable) {
 *             try {
 *                 ((AutoCloseable) dataSource).close();
 *             } catch (Exception e) {
 *                 throw new LifecycleException("Failed to close data source", e);
 *             }
 *         }
 *
 *         started = false;
 *         System.out.println("Database service stopped");
 *     }
 *
 *     @Override
 *     public void onReload() throws LifecycleException {
 *         System.out.println("Reloading database configuration");
 *         onStop();
 *         onInit();
 *         onStart();
 *     }
 *
 *     @Override
 *     public void onFlush() throws LifecycleException {
 *         System.out.println("Flushing database connection pool");
 *         // Evict idle connections from pool
 *     }
 * }
 * }</pre>
 *
 * <h2>Integration with DI</h2>
 * <pre>{@code
 * IInjectionContext context = new InjectionContextBuilder()
 *     .addBean(DatabaseService.class)
 *     .addBean(UserService.class)
 *     .build();
 *
 * // Initialize all beans
 * context.onInit();
 *
 * // Start all beans
 * context.onStart();
 *
 * // Use beans...
 * UserService userService = context.getBean(UserService.class);
 *
 * // Shutdown
 * context.onStop();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Thread-safe state transitions</li>
 *   <li>Hook-based lifecycle management</li>
 *   <li>Exception handling during transitions</li>
 *   <li>Reload support for hot-reload scenarios</li>
 *   <li>Flush support for graceful cleanup</li>
 * </ul>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Predictable initialization</b> - Clear order of operations</li>
 *   <li><b>Graceful shutdown</b> - Proper resource cleanup</li>
 *   <li><b>Hot reload</b> - Update configuration without downtime</li>
 *   <li><b>Consistency</b> - Uniform lifecycle across all components</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.lifecycle.ILifecycle
 */
package com.garganttua.core.lifecycle;
