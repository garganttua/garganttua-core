/**
 * Fluent builder APIs for configuring and constructing dependency injection contexts.
 *
 * <h2>Overview</h2>
 * <p>
 * This package defines fluent DSL interfaces for building DI contexts, bean providers,
 * property providers, and configuring injection behaviors. It provides a type-safe,
 * readable way to configure complex dependency injection scenarios.
 * </p>
 *
 * <h2>Core Builder Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.dsl.IInjectionContextBuilder} - Main context builder</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl.IBeanProviderBuilder} - Bean source configuration</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl.IBeanFactoryBuilder} - Bean factory configuration</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl.IPropertyProviderBuilder} - Property source configuration</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder} - Bean supplier configuration</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder} - Property supplier configuration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a complete DI context
 * IInjectionContext context = new InjectionContextBuilder()
 *     .withPackage("com.myapp.services")
 *     .withPackage("com.myapp.repositories")
 *
 *     // Configure bean providers
 *     .beanProvider("database")
 *         .addBean(DataSource.class)
 *         .addBean(TransactionManager.class)
 *         .done()
 *
 *     // Configure property providers
 *     .propertyProvider("config")
 *         .addProperty("app.name", "MyApplication")
 *         .addProperty("app.version", "1.0.0")
 *         .addPropertiesFromFile("application.properties")
 *         .done()
 *
 *     // Configure bean factory
 *     .beanProvider()
 *         .factory()
 *             .constructor()
 *                 .parameter(0).value("jdbc:mysql://localhost:3306/mydb")
 *                 .parameter(1).property("db.username")
 *                 .parameter(2).property("db.password")
 *                 .done()
 *             .field("timeout").value(30)
 *             .done()
 *         .done()
 *
 *     .withQualifier(Named.class)
 *     .build();
 * }</pre>
 *
 * <h2>Bean Configuration</h2>
 * <p>
 * Configure how beans are created and injected:
 * </p>
 * <pre>{@code
 * beanProvider("services")
 *     .factory(UserService.class)
 *         .constructor()
 *             .parameter(0).bean(UserRepository.class)
 *             .parameter(1).property("user.cache.size")
 *             .done()
 *         .field("logger")
 *             .bean(Logger.class, "userLogger")
 *             .done()
 *         .postConstruct()
 *             .method("initialize")
 *             .done()
 *         .done()
 *     .done();
 * }</pre>
 *
 * <h2>Property Configuration</h2>
 * <p>
 * Configure property sources and resolution:
 * </p>
 * <pre>{@code
 * propertyProvider("application")
 *     .addProperty("db.url", "jdbc:postgresql://localhost:5432/mydb")
 *     .addProperty("db.pool.size", "10")
 *     .addPropertiesFromFile("config/database.properties")
 *     .addPropertiesFromEnvironment()
 *     .done();
 * }</pre>
 *
 * <h2>Advanced Configuration</h2>
 * <p>
 * Support for complex scenarios:
 * </p>
 * <ul>
 *   <li>Package scanning for auto-discovery</li>
 *   <li>Custom qualifiers for bean disambiguation</li>
 *   <li>Parent-child context hierarchies</li>
 *   <li>Custom injectable element resolvers</li>
 *   <li>Build observers for context lifecycle events</li>
 * </ul>
 *
 * <h2>Builder Pattern</h2>
 * <p>
 * All builders follow these conventions:
 * </p>
 * <ul>
 *   <li>Method chaining for fluent configuration</li>
 *   <li>{@code done()} returns to parent builder</li>
 *   <li>{@code build()} creates final object</li>
 *   <li>Type-safe parameter binding</li>
 *   <li>Clear separation of concerns</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.context.dsl.IInjectionContextBuilder
 * @see com.garganttua.core.injection.IInjectionContext
 * @see com.garganttua.core.dsl.IAutomaticBuilder
 */
package com.garganttua.core.injection.context.dsl;
