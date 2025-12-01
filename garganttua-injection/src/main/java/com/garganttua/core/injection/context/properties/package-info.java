/**
 * Property resolution and management implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the implementation for property resolution, including property
 * providers, property suppliers, and the property management system. It handles property
 * placeholder resolution (${property.name}), default values, and multiple property sources.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code PropertyProvider} - Manages collections of property suppliers</li>
 *   <li>{@code PropertySupplier} - Provides individual property values</li>
 *   <li>{@code Properties} - Property utility and management class</li>
 * </ul>
 *
 * <h2>Property Resolution</h2>
 * <p>
 * Properties can come from multiple sources:
 * </p>
 * <ul>
 *   <li>Programmatically added properties</li>
 *   <li>Property files (application.properties, etc.)</li>
 *   <li>System properties</li>
 *   <li>Environment variables</li>
 *   <li>Custom property suppliers</li>
 * </ul>
 *
 * <h2>Usage Example: Property Provider</h2>
 * <pre>{@code
 * // Create property provider
 * PropertyProvider provider = new PropertyProvider("application");
 *
 * // Add properties
 * provider.addProperty("app.name", "MyApplication");
 * provider.addProperty("app.version", "1.0.0");
 * provider.addProperty("db.url", "jdbc:mysql://localhost:3306/mydb");
 *
 * // Load from file
 * provider.loadPropertiesFromFile("application.properties");
 *
 * // Get property
 * String appName = provider.getProperty("app.name");
 * String dbUrl = provider.getProperty("db.url", "jdbc:mysql://localhost:3306/default");
 * }</pre>
 *
 * <h2>Usage Example: Property Supplier</h2>
 * <pre>{@code
 * // Fixed value supplier
 * PropertySupplier nameSupplier = new PropertySupplier("app.name", "MyApplication");
 *
 * // Dynamic supplier
 * PropertySupplier timestampSupplier = new PropertySupplier(
 *     "app.timestamp",
 *     () -> String.valueOf(System.currentTimeMillis())
 * );
 *
 * // Environment variable supplier
 * PropertySupplier pathSupplier = new PropertySupplier(
 *     "app.home",
 *     () -> System.getenv("APP_HOME")
 * );
 *
 * // Get values
 * String name = nameSupplier.get();
 * String timestamp = timestampSupplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Placeholder Resolution</h2>
 * <pre>{@code
 * PropertyProvider provider = new PropertyProvider("config");
 * provider.addProperty("db.host", "localhost");
 * provider.addProperty("db.port", "3306");
 * provider.addProperty("db.name", "mydb");
 *
 * // Resolve placeholder
 * String url = provider.resolvePlaceholder(
 *     "jdbc:mysql://${db.host}:${db.port}/${db.name}"
 * );
 * // Result: "jdbc:mysql://localhost:3306/mydb"
 * }</pre>
 *
 * <h2>Usage Example: Property Hierarchy</h2>
 * <pre>{@code
 * // Parent properties
 * PropertyProvider parent = new PropertyProvider("parent");
 * parent.addProperty("app.name", "BaseApp");
 * parent.addProperty("app.timeout", "30");
 *
 * // Child properties (overrides parent)
 * PropertyProvider child = new PropertyProvider("child", parent);
 * child.addProperty("app.name", "CustomApp");  // Overrides parent
 *
 * // Child gets own value
 * String name = child.getProperty("app.name");  // "CustomApp"
 *
 * // Child falls back to parent
 * String timeout = child.getProperty("app.timeout");  // "30"
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Multiple property sources</li>
 *   <li>Placeholder resolution (${property.name})</li>
 *   <li>Default value support</li>
 *   <li>Property hierarchy (parent-child)</li>
 *   <li>Dynamic property suppliers</li>
 *   <li>Property file loading</li>
 *   <li>Environment variable integration</li>
 *   <li>System property integration</li>
 *   <li>Type conversion</li>
 *   <li>Nested placeholder resolution</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.properties.resolver} - Property value resolution strategies</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IPropertyProvider
 * @see com.garganttua.core.injection.context
 * @see com.garganttua.core.injection.annotations.Property
 */
package com.garganttua.core.injection.context.properties;
