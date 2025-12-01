/**
 * Object supplier and lazy provisioning framework for deferred object creation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides contracts for lazy object provisioning and contextual object supply.
 * Suppliers enable deferred creation of objects until they are needed, supporting dependency
 * injection, factory patterns, and object pooling scenarios.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.supply.IObjectSupplier} - Basic object supplier</li>
 *   <li>{@link com.garganttua.core.supply.IContextualObjectSupplier} - Context-aware supplier</li>
 *   <li>{@link com.garganttua.core.supply.ISupplierFactory} - Factory for creating suppliers</li>
 * </ul>
 *
 * <h2>Supplier Types</h2>
 * <ul>
 *   <li><b>Fixed Supplier</b> - Returns a pre-defined object instance</li>
 *   <li><b>Factory Supplier</b> - Creates new instances on each invocation</li>
 *   <li><b>Lazy Supplier</b> - Creates instance only on first access</li>
 *   <li><b>Contextual Supplier</b> - Creates instances based on execution context</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Fixed Supplier</h3>
 * <pre>{@code
 * User user = new User("John");
 * IObjectSupplier<User> supplier = FixedObjectSupplierBuilder
 *     .create(user)
 *     .build();
 *
 * User retrieved = supplier.get(); // Returns the same instance
 * }</pre>
 *
 * <h3>Factory Supplier</h3>
 * <pre>{@code
 * IObjectSupplier<UUID> uuidSupplier = FactoryObjectSupplierBuilder
 *     .create(UUID::randomUUID, UUID.class)
 *     .build();
 *
 * UUID id1 = uuidSupplier.get(); // New UUID
 * UUID id2 = uuidSupplier.get(); // Different UUID
 * }</pre>
 *
 * <h3>Contextual Supplier</h3>
 * <pre>{@code
 * IContextualObjectSupplier<User> userSupplier = ContextualObjectSupplierBuilder
 *     .create(ctx -> {
 *         String userId = ctx.getProperty("userId");
 *         return userRepository.findById(userId);
 *     }, User.class)
 *     .build();
 *
 * User user = userSupplier.get(executionContext);
 * }</pre>
 *
 * <h2>Integration with DI</h2>
 * <pre>{@code
 * // Register supplier as bean provider
 * IDiContext context = new DiContextBuilder()
 *     .addBeanSupplier(User.class, userSupplier)
 *     .build();
 *
 * User user = context.getBean(User.class); // Uses supplier
 * }</pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Lazy loading</b> - Defer expensive object creation</li>
 *   <li><b>Decoupling</b> - Separate object creation from usage</li>
 *   <li><b>Testability</b> - Easy mocking and stubbing</li>
 *   <li><b>Flexibility</b> - Switch implementations at runtime</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.supply.dsl} - Fluent builder APIs for supplier creation</li>
 *   <li>{@link com.garganttua.core.injection} - DI integration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.supply.IObjectSupplier
 * @see com.garganttua.core.supply.IContextualObjectSupplier
 */
package com.garganttua.core.supply;
