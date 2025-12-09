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
 *   <li>{@code FixedSupplier} - Supplies fixed value</li>
 *   <li>{@code NullSupplier} - Supplies null value</li>
 *   <li>{@code NewSupplier} - Creates new object instances</li>
 *   <li>{@code NullableSupplier} - Supplies value or null</li>
 *   <li>{@code ContextualSupplier} - Supplies value from context</li>
 *   <li>{@code NewContextualSupplier} - Creates objects with context</li>
 *   <li>{@code NullableContextualSupplier} - Context-aware nullable supplier</li>
 * </ul>
 *
 * <h2>Usage Example: Fixed Supplier</h2>
 * <pre>{@code
 * // Create fixed value supplier
 * ISupplier<String> supplier = new FixedSupplier<>("default-value");
 *
 * // Get value
 * String value = supplier.get();  // Always returns "default-value"
 * }</pre>
 *
 * <h2>Usage Example: New Object Supplier</h2>
 * <pre>{@code
 * // Create supplier that creates new instances
 * ISupplier<User> userSupplier = new NewSupplier<>(
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
 * ISupplier<UserRepository> repoSupplier =
 *     new ContextualSupplier<>(
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
 * ISupplier<String> supplier = new NullableSupplier<>(
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
