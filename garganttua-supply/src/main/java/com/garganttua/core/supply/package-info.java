/**
 * Value supplier framework implementation for lazy and dynamic value resolution.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the value supplier framework.
 * It implements various supplier types that provide values dynamically, supporting
 * lazy evaluation, context-awareness, and object construction.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code FixedSupplier} - Supplies fixed value</li>
 *   <li>{@code NullSupplier} - Supplies null value</li>
 *   <li>{@code NewSupplier} - Creates new object instances</li>
 *   <li>{@code NullableSupplier} - Wrapper that validates nullable behavior</li>
 *   <li>{@code ContextualSupplier} - Supplies value from context</li>
 *   <li>{@code NewContextualSupplier} - Creates objects with context</li>
 *   <li>{@code NullableContextualSupplier} - Context-aware nullable supplier</li>
 * </ul>
 *
 * <h2>Usage Example: Fixed Value Supplier (from SupplierTest)</h2>
 * <pre>{@code
 * import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
 * import com.garganttua.core.supply.ISupplier;
 *
 * FixedSupplierBuilder<String> builder = new FixedSupplierBuilder<String>("hello");
 * ISupplier<String> supplier = builder.build();
 *
 * assertEquals("hello", supplier.supply().get());
 * }</pre>
 *
 * <h2>Usage Example: Contextual Supplier with Lambda (from SupplierTest)</h2>
 * <pre>{@code
 * import com.garganttua.core.supply.IContextualSupply;
 * import com.garganttua.core.supply.IContextualSupplier;
 * import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;
 *
 * IContextualSupply<String, Object> supply = (context, contexts) ->
 *     Optional.of("hello from context");
 *
 * ISupplierBuilder<String, IContextualSupplier<String, Object>> builder =
 *     new ContextualSupplierBuilder<String, Object>(supply, String.class, Object.class);
 *
 * IContextualSupplier<String, Object> supplier = builder.build();
 * assertEquals("hello from context", supplier.supply(new Object()).get());
 * }</pre>
 *
 * <h2>Usage Example: SupplierBuilder with Value (from SupplierBuilderTest)</h2>
 * <pre>{@code
 * import com.garganttua.core.supply.dsl.SupplierBuilder;
 *
 * var b = new SupplierBuilder<>(String.class).withValue("hello");
 * var s = b.build();
 * // Returns NullableSupplier wrapping FixedSupplier
 * }</pre>
 *
 * <h2>Usage Example: Custom Context Type (from SupplierTest)</h2>
 * <pre>{@code
 * IContextualSupply<String, String> supply = (context, contexts) ->
 *     Optional.of("hello from context " + context);
 *
 * ContextualSupplierBuilder<String, String> builder =
 *     new ContextualSupplierBuilder<String, String>(supply, String.class, String.class);
 *
 * IContextualSupplier<String, String> supplier = builder.build();
 * assertEquals("hello from context string context",
 *     supplier.supply("string context").get());
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Lazy evaluation with Optional-based API</li>
 *   <li>Fixed value suppliers for constants</li>
 *   <li>Null value handling with NullSupplier</li>
 *   <li>Dynamic object creation via constructor binders</li>
 *   <li>Context-aware resolution for DI integration</li>
 *   <li>Type-safe suppliers with generic parameters</li>
 *   <li>Nullable wrappers for runtime validation</li>
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
