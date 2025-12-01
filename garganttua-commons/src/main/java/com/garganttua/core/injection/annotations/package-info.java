/**
 * Dependency injection annotations for bean configuration and property resolution.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides annotations that complement JSR-330 standard annotations,
 * adding property injection, provider methods, scope control, and qualifier capabilities
 * to the Garganttua dependency injection framework.
 * </p>
 *
 * <h2>Core Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.annotations.Property} - Injects configuration properties using ${property.name} placeholders</li>
 *   <li>{@link com.garganttua.core.injection.annotations.Provider} - Marks factory methods that produce beans</li>
 *   <li>{@link com.garganttua.core.injection.annotations.Prototype} - Declares prototype-scoped beans (new instance per request)</li>
 *   <li>{@link com.garganttua.core.injection.annotations.Fixed} - Marks fixed value injections</li>
 *   <li>{@link com.garganttua.core.injection.annotations.Null} - Explicitly injects null values</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Property Injection</h3>
 * <pre>{@code
 * public class DatabaseService {
 *     @Inject
 *     @Property("db.url")
 *     private String databaseUrl;
 *
 *     @Inject
 *     @Property("db.timeout")
 *     private int timeout;
 * }
 * }</pre>
 *
 * <h3>Provider Methods</h3>
 * <pre>{@code
 * public class DataSourceProvider {
 *     @Provider
 *     public DataSource createDataSource(
 *         @Property("db.url") String url,
 *         @Property("db.username") String user,
 *         @Property("db.password") String password) {
 *         return new HikariDataSource(url, user, password);
 *     }
 * }
 * }</pre>
 *
 * <h3>Prototype Scope</h3>
 * <pre>{@code
 * @Prototype
 * public class SessionHandler {
 *     // New instance created for each injection
 * }
 * }</pre>
 *
 * <h3>Fixed Values</h3>
 * <pre>{@code
 * public class Configuration {
 *     @Inject
 *     @Fixed("production")
 *     private String environment;
 * }
 * }</pre>
 *
 * <h2>Integration with JSR-330</h2>
 * <p>
 * These annotations work alongside standard JSR-330 annotations:
 * </p>
 * <ul>
 *   <li>Use {@code @Inject} with {@code @Property} for property injection</li>
 *   <li>Use {@code @Inject} with {@code @Qualifier} for bean disambiguation</li>
 *   <li>Use {@code @Provider} on methods to create complex beans</li>
 *   <li>Use {@code @Prototype} to override default singleton scope</li>
 * </ul>
 *
 * <h2>Property Resolution</h2>
 * <p>
 * Properties are resolved from various sources:
 * </p>
 * <ul>
 *   <li>Context property providers</li>
 *   <li>System properties</li>
 *   <li>Environment variables</li>
 *   <li>Property files (application.properties, etc.)</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection
 * @see com.garganttua.core.injection.context.dsl
 * @see javax.inject.Inject
 * @see javax.inject.Qualifier
 */
package com.garganttua.core.injection.annotations;
