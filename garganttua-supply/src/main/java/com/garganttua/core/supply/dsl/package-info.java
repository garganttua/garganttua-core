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
 * <h2>Usage Example: Supplier Builder</h2>
 * <pre>{@code
 * // Build fixed value supplier
 * IObjectSupplier<String> supplier = new ObjectSupplierBuilder<String>()
 *     .value("default-value")
 *     .build();
 *
 * // Build property supplier
 * IObjectSupplier<String> urlSupplier = new ObjectSupplierBuilder<String>()
 *     .property("api.url")
 *     .defaultValue("http://localhost:8080")
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Custom Supplier Builder</h2>
 * <pre>{@code
 * // Build supplier with custom logic
 * IObjectSupplier<String> timestampSupplier =
 *     new ObjectSupplierBuilder<String>()
 *         .custom(() -> {
 *             LocalDateTime now = LocalDateTime.now();
 *             return now.format(DateTimeFormatter.ISO_DATE_TIME);
 *         })
 *         .build();
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
