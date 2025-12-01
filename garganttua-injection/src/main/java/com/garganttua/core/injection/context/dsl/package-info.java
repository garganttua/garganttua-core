/**
 * Fluent builder API implementations for DI context configuration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of the fluent DSL interfaces defined
 * in garganttua-commons for building DI contexts. It implements the builder pattern
 * to provide a type-safe, readable API for configuring dependency injection contexts.
 * </p>
 *
 * <h2>Implementation Classes</h2>
 * <p>
 * This package contains implementations of the builder interfaces from
 * {@link com.garganttua.core.injection.context.dsl} (commons package).
 * </p>
 *
 * <h2>Usage Example: Complete Context Configuration</h2>
 * <pre>{@code
 * IDiContext context = new DiContextBuilder()
 *     // Package scanning
 *     .withPackage("com.myapp.services")
 *     .withPackage("com.myapp.repositories")
 *     .withPackages(new String[]{"com.myapp.controllers", "com.myapp.utils"})
 *
 *     // Bean providers
 *     .beanProvider("database")
 *         .addBean(DataSource.class)
 *         .addBean(TransactionManager.class)
 *         .addBean(JdbcTemplate.class)
 *         .done()
 *
 *     // Property providers
 *     .propertyProvider("application")
 *         .addProperty("app.name", "MyApplication")
 *         .addProperty("app.version", "1.0.0")
 *         .addPropertiesFromFile("application.properties")
 *         .done()
 *
 *     // Custom qualifiers
 *     .withQualifier(Named.class)
 *     .withQualifier(Primary.class)
 *
 *     // Build observers
 *     .observer(buildEvent -> {
 *         System.out.println("Context building: " + buildEvent);
 *     })
 *
 *     // Build context
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Bean Factory Configuration</h2>
 * <pre>{@code
 * IDiContext context = new DiContextBuilder()
 *     .beanProvider("services")
 *         .factory(UserService.class)
 *             // Constructor injection
 *             .constructor()
 *                 .parameter(0).bean(UserRepository.class)
 *                 .parameter(1).property("user.cache.size")
 *                 .done()
 *
 *             // Field injection
 *             .field("logger")
 *                 .bean(Logger.class, "userLogger")
 *                 .done()
 *
 *             // Post-construct method
 *             .postConstruct()
 *                 .method("initialize")
 *                 .done()
 *
 *             .done()
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Property Supplier</h2>
 * <pre>{@code
 * IDiContext context = new DiContextBuilder()
 *     .propertyProvider("config")
 *         .supplier("db.url")
 *             .value("jdbc:mysql://localhost:3306/mydb")
 *             .done()
 *
 *         .supplier("db.pool.size")
 *             .value(10)
 *             .done()
 *
 *         .supplier("db.timeout")
 *             .custom(() -> calculateTimeout())
 *             .done()
 *
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Bean Supplier</h2>
 * <pre>{@code
 * IDiContext context = new DiContextBuilder()
 *     .beanProvider("datasource")
 *         .supplier(DataSource.class)
 *             .type(HikariDataSource.class)
 *             .constructor()
 *                 .parameter(0).property("db.url")
 *                 .done()
 *             .property("maximumPoolSize").value(10)
 *             .done()
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Injectable Field Configuration</h2>
 * <pre>{@code
 * IDiContext context = new DiContextBuilder()
 *     .beanProvider("services")
 *         .factory(EmailService.class)
 *             .injectableField("smtpHost")
 *                 .property("smtp.host")
 *                 .done()
 *
 *             .injectableField("smtpPort")
 *                 .property("smtp.port")
 *                 .defaultValue(587)
 *                 .done()
 *
 *             .injectableField("mailSender")
 *                 .bean(MailSender.class)
 *                 .done()
 *
 *             .done()
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe configuration</li>
 *   <li>Package scanning support</li>
 *   <li>Bean factory configuration</li>
 *   <li>Property supplier configuration</li>
 *   <li>Bean supplier configuration</li>
 *   <li>Injectable field configuration</li>
 *   <li>Qualifier support</li>
 *   <li>Build observation hooks</li>
 *   <li>Automatic resolver configuration</li>
 * </ul>
 *
 * <h2>Builder Hierarchy</h2>
 * <p>
 * Builders follow a clear hierarchy:
 * </p>
 * <ul>
 *   <li><b>DiContextBuilder</b> - Root builder
 *     <ul>
 *       <li><b>BeanProviderBuilder</b> - Bean source configuration
 *         <ul>
 *           <li><b>BeanFactoryBuilder</b> - Bean factory details</li>
 *           <li><b>BeanSupplierBuilder</b> - Bean supplier details</li>
 *         </ul>
 *       </li>
 *       <li><b>PropertyProviderBuilder</b> - Property source configuration
 *         <ul>
 *           <li><b>PropertySupplierBuilder</b> - Property supplier details</li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.context.dsl
 * @see com.garganttua.core.injection.context
 * @see com.garganttua.core.injection.IDiContext
 */
package com.garganttua.core.injection.context.dsl;
