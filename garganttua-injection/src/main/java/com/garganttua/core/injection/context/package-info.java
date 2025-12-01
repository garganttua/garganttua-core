/**
 * Dependency injection context implementation and management.
 *
 * <h2>Overview</h2>
 * <p>
 * This package contains the core implementation of the DI context ({@code DiContext}),
 * which orchestrates bean lifecycle, property resolution, dependency injection, and
 * context hierarchy management. It serves as the central coordinator for all DI operations.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code DiContext} - Main DI context implementation managing beans and properties</li>
 *   <li>{@code Predefined} - Predefined bean and property constants</li>
 * </ul>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Bean lifecycle management (creation, initialization, destruction)</li>
 *   <li>Dependency graph resolution and injection</li>
 *   <li>Property placeholder resolution</li>
 *   <li>Scope management (singleton vs prototype)</li>
 *   <li>Context hierarchy (parent-child relationships)</li>
 *   <li>Circular dependency detection</li>
 *   <li>Post-construct and pre-destroy callback execution</li>
 * </ul>
 *
 * <h2>Context Lifecycle</h2>
 * <pre>{@code
 * // 1. Create context
 * IDiContext context = new DiContextBuilder()
 *     .withPackage("com.myapp")
 *     .build();
 *
 * // 2. Start context (initializes beans, executes @PostConstruct)
 * context.onStart();
 *
 * // 3. Use context
 * MyService service = context.getBean(MyService.class);
 * service.doWork();
 *
 * // 4. Stop context (executes @PreDestroy, cleans up resources)
 * context.onStop();
 * }</pre>
 *
 * <h2>Bean Resolution</h2>
 * <pre>{@code
 * // By type
 * UserService service = context.getBean(UserService.class);
 *
 * // By type and qualifier
 * UserService adminService = context.getBean(UserService.class, "admin");
 *
 * // Check existence
 * boolean hasBean = context.hasBean(UserService.class);
 * }</pre>
 *
 * <h2>Property Resolution</h2>
 * <pre>{@code
 * // Get property
 * String dbUrl = context.getProperty("db.url");
 *
 * // Get with default
 * String dbUrl = context.getProperty("db.url", "jdbc:mysql://localhost:3306/default");
 *
 * // Check existence
 * boolean hasProperty = context.hasProperty("db.url");
 * }</pre>
 *
 * <h2>Context Hierarchy</h2>
 * <pre>{@code
 * // Create parent context
 * IDiContext parent = new DiContextBuilder()
 *     .withPackage("com.myapp.core")
 *     .build();
 *
 * // Create child context
 * IDiContext child = new DiContextBuilder()
 *     .withPackage("com.myapp.module")
 *     .parentContext(parent)
 *     .build();
 *
 * // Child can access parent beans
 * CoreService coreService = child.getBean(CoreService.class);
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.beans} - Bean management implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.properties} - Property resolution implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl} - Context builder implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.validation} - Validation implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.resolver} - Element resolver implementation</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IDiContext
 * @see com.garganttua.core.injection.context.beans
 * @see com.garganttua.core.injection.context.dsl
 */
package com.garganttua.core.injection.context;
