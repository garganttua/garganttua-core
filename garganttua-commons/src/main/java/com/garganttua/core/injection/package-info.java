/**
 * Dependency injection framework contracts providing JSR-330 compatible IoC container interfaces.
 *
 * <h2>Overview</h2>
 * <p>
 * This package defines the core contracts for the Garganttua dependency injection framework.
 * It provides a lightweight, modular IoC container supporting singleton and prototype scopes,
 * property injection, provider methods, and hierarchical contexts.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.IDiContext} - Central DI container managing beans and properties</li>
 *   <li>{@link com.garganttua.core.injection.IBeanProvider} - Bean source abstraction for different strategies</li>
 *   <li>{@link com.garganttua.core.injection.IBeanFactory} - Factory for creating bean instances</li>
 *   <li>{@link com.garganttua.core.injection.IBeanQuery} - Query interface for bean lookup</li>
 *   <li>{@link com.garganttua.core.injection.IPropertyProvider} - Property source abstraction</li>
 * </ul>
 *
 * <h2>Bean Strategies</h2>
 * <p>
 * The framework supports multiple bean instantiation strategies via {@link com.garganttua.core.injection.BeanStrategy}:
 * </p>
 * <ul>
 *   <li><b>SINGLETON</b> - Single instance per context (default)</li>
 *   <li><b>PROTOTYPE</b> - New instance on each request</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Constructor, field, and setter injection</li>
 *   <li>Qualifier-based bean disambiguation</li>
 *   <li>Property placeholder resolution (${property.name})</li>
 *   <li>Parent-child context hierarchies</li>
 *   <li>Provider methods for complex bean creation</li>
 *   <li>Lifecycle callbacks (post-construct, pre-destroy)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create context
 * IDiContext context = new DiContextBuilder()
 *     .addBean(UserRepository.class)
 *     .addBean(UserService.class)
 *     .addProperty("db.url", "jdbc:mysql://localhost:3306/mydb")
 *     .build();
 *
 * // Start lifecycle
 * context.onStart();
 *
 * // Retrieve beans
 * UserService service = context.getBean(UserService.class);
 *
 * // With qualifier
 * UserService adminService = context.getBean(UserService.class, "admin");
 *
 * // Get property
 * String dbUrl = context.getProperty("db.url");
 *
 * // Shutdown
 * context.onStop();
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.annotations} - Injection annotations (@Inject, @Qualifier, etc.)</li>
 *   <li>{@link com.garganttua.core.injection.context} - Context implementation support</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl} - Fluent builder APIs for context configuration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IDiContext
 * @see com.garganttua.core.injection.IBeanProvider
 * @see com.garganttua.core.injection.context.dsl
 */
package com.garganttua.core.injection;
