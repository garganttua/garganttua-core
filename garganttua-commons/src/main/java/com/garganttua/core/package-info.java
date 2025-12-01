/**
 * Garganttua Core framework - Foundation module providing shared components, interfaces,
 * annotations, and exceptions for all Garganttua modules.
 *
 * <h2>Overview</h2>
 * <p>
 * This package contains the foundational contracts and common utilities used across the entire
 * Garganttua ecosystem. It defines the core abstractions for dependency injection, runtime
 * orchestration, reflection, object mapping, condition evaluation, and more.
 * </p>
 *
 * <h2>Key Modules</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection} - Dependency injection contracts (JSR-330 compatible)</li>
 *   <li>{@link com.garganttua.core.runtime} - Runtime workflow orchestration interfaces</li>
 *   <li>{@link com.garganttua.core.reflection} - Advanced reflection and binding abstractions</li>
 *   <li>{@link com.garganttua.core.dsl} - Domain-Specific Language builder framework</li>
 *   <li>{@link com.garganttua.core.supply} - Object supplier and provisioning contracts</li>
 *   <li>{@link com.garganttua.core.condition} - Condition evaluation framework</li>
 *   <li>{@link com.garganttua.core.execution} - Chain-of-responsibility execution patterns</li>
 *   <li>{@link com.garganttua.core.mapper} - Object-to-object mapping contracts</li>
 *   <li>{@link com.garganttua.core.lifecycle} - Lifecycle management interfaces</li>
 *   <li>{@link com.garganttua.core.crypto} - Cryptography and hashing utilities</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <p>
 * The commons module follows these design principles:
 * </p>
 * <ul>
 *   <li><b>Interface segregation</b> - Small, focused interfaces following the ISP principle</li>
 *   <li><b>Type safety</b> - Extensive use of generics for compile-time type checking</li>
 *   <li><b>Immutability</b> - Records and immutable objects where appropriate</li>
 *   <li><b>Optional over null</b> - {@link java.util.Optional} for nullable results</li>
 *   <li><b>Checked exceptions</b> - Explicit error handling for business logic failures</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a dependency injection context
 * IDiContext context = new DiContextBuilder()
 *     .addBean(UserService.class)
 *     .addProperty("app.name", "MyApp")
 *     .build();
 *
 * // Start lifecycle
 * context.onStart();
 *
 * // Use beans
 * UserService service = context.getBean(UserService.class);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IDiContext
 * @see com.garganttua.core.runtime.IRuntime
 * @see com.garganttua.core.reflection.IBinder
 */
package com.garganttua.core;
