/**
 * Value supplier framework implementation for lazy and dynamic value resolution.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the value supplier framework.
 * It implements various supplier types that provide values dynamically, supporting
 * lazy evaluation, caching, context-awareness, and object construction.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code FixedObjectSupplier} - Supplies fixed value</li>
 *   <li>{@code NullObjectSupplier} - Supplies null value</li>
 *   <li>{@code NewObjectSupplier} - Creates new object instances</li>
 *   <li>{@code NullableObjectSupplier} - Supplies value or null</li>
 *   <li>{@code ContextualObjectSupplier} - Supplies value from context</li>
 *   <li>{@code NewContextualObjectSupplier} - Creates objects with context</li>
 *   <li>{@code NullableContextualObjectSupplier} - Context-aware nullable supplier</li>
 * </ul>
 *
 * <h2>Usage Example: Fixed Supplier</h2>
 * <pre>{@code
 * // Create fixed value supplier
 * IObjectSupplier<String> supplier = new FixedObjectSupplier<>("default-value");
 *
 * // Get value
 * String value = supplier.get();  // Always returns "default-value"
 * }</pre>
 *
 * <h2>Usage Example: New Object Supplier</h2>
 * <pre>{@code
 * // Create supplier that creates new instances
 * IObjectSupplier<User> userSupplier = new NewObjectSupplier<>(
 *     User.class,
 *     () -> {
 *         User user = new User();
 *         user.setStatus("NEW");
 *         return user;
 *     }
 * );
 *
 * // Get new instance on each call
 * User user1 = userSupplier.get();
 * User user2 = userSupplier.get();  // Different instance
 * }</pre>
 *
 * <h2>Usage Example: Contextual Supplier</h2>
 * <pre>{@code
 * // Create context-aware supplier
 * IObjectSupplier<UserRepository> repoSupplier =
 *     new ContextualObjectSupplier<>(
 *         context -> context.getBean(UserRepository.class)
 *     );
 *
 * // Get value from context
 * UserRepository repository = repoSupplier.get(diContext);
 * }</pre>
 *
 * <h2>Usage Example: Nullable Supplier</h2>
 * <pre>{@code
 * // Create nullable supplier
 * IObjectSupplier<String> supplier = new NullableObjectSupplier<>(
 *     () -> {
 *         String value = fetchFromCache();
 *         return value != null ? value : null;
 *     }
 * );
 *
 * // May return null
 * String value = supplier.get();
 * if (value != null) {
 *     // Use value
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Lazy evaluation</li>
 *   <li>Optional caching</li>
 *   <li>Fixed value suppliers</li>
 *   <li>Null value handling</li>
 *   <li>Dynamic object creation</li>
 *   <li>Context-aware resolution</li>
 *   <li>Type-safe suppliers</li>
 *   <li>Reusable supplier objects</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.supply.dsl} - Fluent builder implementations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.supply.dsl
 */
package com.garganttua.core.supply;
