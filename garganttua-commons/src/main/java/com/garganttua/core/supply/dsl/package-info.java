/**
 * Fluent builder APIs for constructing value suppliers and object suppliers.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides fluent DSL interfaces for building suppliers that provide
 * values dynamically. Suppliers enable lazy evaluation, caching, and context-aware
 * value resolution for dependency injection and runtime execution scenarios.
 * </p>
 *
 * <h2>Core Builder Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.supply.dsl.ISupplierBuilder} - Generic value supplier builder</li>
 *   <li>{@link com.garganttua.core.supply.dsl.IObjectSupplierBuilder} - Object-specific supplier builder</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Supplier</h2>
 * <pre>{@code
 * // Build a simple value supplier
 * ISupplier<String> supplier = new SupplierBuilder<String>()
 *     .value("default-value")
 *     .build();
 *
 * // Get value
 * String value = supplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Property Supplier</h2>
 * <pre>{@code
 * // Build supplier that resolves from properties
 * ISupplier<String> urlSupplier = new SupplierBuilder<String>()
 *     .property("api.url")
 *     .defaultValue("http://localhost:8080")
 *     .build();
 *
 * // Value resolved from property source
 * String apiUrl = urlSupplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Bean Supplier</h2>
 * <pre>{@code
 * // Build supplier that retrieves beans from DI context
 * ISupplier<UserRepository> repoSupplier = new SupplierBuilder<UserRepository>()
 *     .bean(UserRepository.class)
 *     .qualifier("primary")
 *     .build();
 *
 * // Get bean instance
 * UserRepository repository = repoSupplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Custom Supplier</h2>
 * <pre>{@code
 * // Build supplier with custom logic
 * ISupplier<String> timestampSupplier = new SupplierBuilder<String>()
 *     .custom(() -> {
 *         LocalDateTime now = LocalDateTime.now();
 *         return now.format(DateTimeFormatter.ISO_DATE_TIME);
 *     })
 *     .build();
 *
 * // Get dynamically generated value
 * String timestamp = timestampSupplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Context-Aware Supplier</h2>
 * <pre>{@code
 * // Build supplier with context access
 * ISupplier<User> currentUserSupplier = new SupplierBuilder<User>()
 *     .fromContext(context -> {
 *         String userId = context.getProperty("current.user.id");
 *         UserRepository repo = context.getBean(UserRepository.class);
 *         return repo.findById(userId);
 *     })
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Object Supplier</h2>
 * <pre>{@code
 * // Build object supplier with construction logic
 * IObjectSupplier<DataSource> dataSourceSupplier =
 *     new ObjectSupplierBuilder<DataSource>()
 *         .type(HikariDataSource.class)
 *         .constructor()
 *             .parameter(0).property("db.url")
 *             .parameter(1).property("db.username")
 *             .parameter(2).property("db.password")
 *             .done()
 *         .property("maximumPoolSize").value(10)
 *         .property("connectionTimeout").value(30000)
 *         .build();
 *
 * // Create configured instance
 * DataSource dataSource = dataSourceSupplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Cached Supplier</h2>
 * <pre>{@code
 * // Build supplier with caching
 * ISupplier<Configuration> configSupplier = new SupplierBuilder<Configuration>()
 *     .custom(() -> loadConfigurationFromFile())
 *     .cached()
 *     .ttl(Duration.ofMinutes(5))
 *     .build();
 *
 * // First call loads, subsequent calls return cached value
 * Configuration config = configSupplier.get();
 * }</pre>
 *
 * <h2>Supplier Types</h2>
 * <ul>
 *   <li><b>Value Supplier</b> - Returns fixed value</li>
 *   <li><b>Property Supplier</b> - Resolves from property sources</li>
 *   <li><b>Bean Supplier</b> - Retrieves from DI context</li>
 *   <li><b>Custom Supplier</b> - Executes custom logic</li>
 *   <li><b>Context Supplier</b> - Context-aware resolution</li>
 *   <li><b>Template Supplier</b> - String template with placeholders</li>
 *   <li><b>Object Supplier</b> - Constructs configured objects</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Lazy evaluation</li>
 *   <li>Optional caching</li>
 *   <li>TTL support for cached values</li>
 *   <li>Property placeholder resolution</li>
 *   <li>Bean dependency injection</li>
 *   <li>Template string support</li>
 *   <li>Default value handling</li>
 *   <li>Type-safe configuration</li>
 *   <li>Context awareness</li>
 *   <li>Reusable supplier objects</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * Suppliers built with this DSL are used by:
 * </p>
 * <ul>
 *   <li>Dependency injection framework for lazy bean initialization</li>
 *   <li>Runtime execution framework for dynamic value resolution</li>
 *   <li>Configuration management for property binding</li>
 *   <li>Factory patterns for object creation</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.supply
 * @see com.garganttua.core.supply.dsl.ISupplierBuilder
 * @see com.garganttua.core.supply.dsl.IObjectSupplierBuilder
 * @see com.garganttua.core.dsl.IAutomaticBuilder
 */
package com.garganttua.core.supply.dsl;
