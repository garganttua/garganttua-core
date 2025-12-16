/**
 * Fluent builder API implementations for constructing value suppliers.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of the fluent DSL interfaces defined
 * in garganttua-commons for building suppliers. It implements builder classes for all
 * supplier types, enabling type-safe, configurable supplier construction.
 * </p>
 *
 * <h2>Usage Example: SupplierBuilder with Value (from SupplierBuilderTest)</h2>
 * <pre>{@code
 * import com.garganttua.core.supply.dsl.SupplierBuilder;
 * import com.garganttua.core.supply.NullableSupplier;
 * import com.garganttua.core.supply.FixedSupplier;
 *
 * var b = new SupplierBuilder<>(String.class).withValue("hello");
 * var s = b.build();
 * // Returns NullableSupplier wrapping FixedSupplier
 * assertTrue(s instanceof NullableSupplier);
 * assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof FixedSupplier);
 * }</pre>
 *
 * <h2>Usage Example: SupplierBuilder with Constructor (from SupplierBuilderTest)</h2>
 * <pre>{@code
 * import com.garganttua.core.supply.dsl.SupplierBuilder;
 * import com.garganttua.core.supply.NullableSupplier;
 * import com.garganttua.core.supply.NewSupplier;
 * import com.garganttua.core.reflection.binders.IConstructorBinder;
 *
 * var b = new SupplierBuilder<>(String.class)
 *     .withConstructor(new FakeConstructorBinder<>());
 *
 * var s = b.build();
 * assertTrue(s instanceof NullableSupplier);
 * assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof NewSupplier);
 * }</pre>
 *
 * <h2>Usage Example: SupplierBuilder with Context (from SupplierBuilderTest)</h2>
 * <pre>{@code
 * import com.garganttua.core.supply.dsl.SupplierBuilder;
 * import com.garganttua.core.supply.NullableContextualSupplier;
 * import com.garganttua.core.supply.ContextualSupplier;
 *
 * var b = new SupplierBuilder<>(String.class)
 *     .withContext(Integer.class, new FakeContextualSupply<>());
 *
 * var s = b.build();
 * assertTrue(s instanceof NullableContextualSupplier);
 * assertTrue(((NullableContextualSupplier<?, ?>) s).getDelegate() instanceof ContextualSupplier);
 * }</pre>
 *
 * <h2>Usage Example: FixedSupplierBuilder (from SupplierTest)</h2>
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
 * <h2>Usage Example: ContextualSupplierBuilder (from SupplierTest)</h2>
 * <pre>{@code
 * import com.garganttua.core.supply.IContextualSupply;
 * import com.garganttua.core.supply.IContextualSupplier;
 * import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;
 *
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
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe configuration</li>
 *   <li>Multiple value sources</li>
 *   <li>Custom logic support</li>
 *   <li>Default value handling</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.supply.dsl
 * @see com.garganttua.core.supply
 */
package com.garganttua.core.supply.dsl;
