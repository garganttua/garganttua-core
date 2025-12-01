/**
 * Bean creation, lifecycle management, and scope handling implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the implementation for bean management, including bean factories,
 * bean providers, bean suppliers, bean queries, and the overall bean lifecycle. It handles
 * singleton and prototype scopes, dependency injection, and bean caching.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code BeanFactory} - Creates bean instances with dependency injection</li>
 *   <li>{@code BeanProvider} - Manages collections of bean factories and suppliers</li>
 *   <li>{@code BeanSupplier} - Provides configured bean instances</li>
 *   <li>{@code BeanQuery} - Queries beans by type and qualifier</li>
 *   <li>{@code Beans} - Bean utility and management class</li>
 * </ul>
 *
 * <h2>Bean Lifecycle</h2>
 * <ol>
 *   <li><b>Discovery</b> - Bean classes discovered via package scanning or explicit registration</li>
 *   <li><b>Registration</b> - Bean definitions registered with provider</li>
 *   <li><b>Instantiation</b> - Bean instance created via factory</li>
 *   <li><b>Dependency Injection</b> - Constructor, field, and setter injection performed</li>
 *   <li><b>Post-Construction</b> - @PostConstruct methods executed</li>
 *   <li><b>Ready</b> - Bean ready for use</li>
 *   <li><b>Pre-Destruction</b> - @PreDestroy methods executed during context shutdown</li>
 *   <li><b>Destroyed</b> - Bean references cleared</li>
 * </ol>
 *
 * <h2>Usage Example: Bean Factory</h2>
 * <pre>{@code
 * // Create bean factory for UserService
 * BeanFactory<UserService> factory = new BeanFactory<>(UserService.class)
 *     .withConstructorInjection(
 *         UserRepository.class,
 *         ConfigurationProperties.class
 *     )
 *     .withFieldInjection("logger", Logger.class)
 *     .withPostConstruct("initialize");
 *
 * // Create instance
 * UserService service = factory.create(diContext);
 * }</pre>
 *
 * <h2>Usage Example: Bean Provider</h2>
 * <pre>{@code
 * // Create bean provider
 * BeanProvider provider = new BeanProvider("services");
 *
 * // Register factories
 * provider.registerFactory(UserService.class, userServiceFactory);
 * provider.registerFactory(OrderService.class, orderServiceFactory);
 *
 * // Get bean
 * UserService service = provider.getBean(UserService.class, diContext);
 *
 * // Check existence
 * boolean hasBean = provider.hasBean(OrderService.class);
 * }</pre>
 *
 * <h2>Usage Example: Bean Supplier</h2>
 * <pre>{@code
 * // Create bean supplier with configuration
 * BeanSupplier<DataSource> supplier = new BeanSupplier<>(
 *     HikariDataSource.class,
 *     diContext
 * )
 *     .configureConstructor(
 *         param -> param.resolveProperty("db.url"),
 *         param -> param.resolveProperty("db.username"),
 *         param -> param.resolveProperty("db.password")
 *     )
 *     .configureProperty("maximumPoolSize", 10)
 *     .configureProperty("connectionTimeout", 30000);
 *
 * // Get configured instance
 * DataSource dataSource = supplier.get();
 * }</pre>
 *
 * <h2>Usage Example: Bean Query</h2>
 * <pre>{@code
 * BeanQuery query = new BeanQuery(beanProvider);
 *
 * // Query by type
 * UserService service = query.findByType(UserService.class);
 *
 * // Query by type and qualifier
 * UserService adminService = query.findByTypeAndQualifier(
 *     UserService.class,
 *     "admin"
 * );
 *
 * // Query all of type
 * List<MessageHandler> handlers = query.findAllByType(MessageHandler.class);
 * }</pre>
 *
 * <h2>Scope Management</h2>
 * <ul>
 *   <li><b>Singleton</b> - One instance per context (cached)</li>
 *   <li><b>Prototype</b> - New instance on each request (not cached)</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Constructor injection with parameter resolution</li>
 *   <li>Field injection with type matching</li>
 *   <li>Setter injection support</li>
 *   <li>Singleton scope with caching</li>
 *   <li>Prototype scope without caching</li>
 *   <li>Circular dependency detection</li>
 *   <li>Lazy initialization</li>
 *   <li>Qualifier-based disambiguation</li>
 *   <li>Post-construct callbacks</li>
 *   <li>Pre-destroy callbacks</li>
 *   <li>Bean query capabilities</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.beans.resolver} - Bean dependency resolution strategies</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IBeanFactory
 * @see com.garganttua.core.injection.IBeanProvider
 * @see com.garganttua.core.injection.context
 */
package com.garganttua.core.injection.context.beans;
