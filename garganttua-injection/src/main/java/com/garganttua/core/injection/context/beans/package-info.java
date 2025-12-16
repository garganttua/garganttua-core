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
 * Based on real test code from BeanFactoryTest.java:
 * <pre>{@code
 * // Create singleton bean factory
 * BeanFactory<DummyBean> singletonFactory = new BeanFactory<>(
 *     new BeanDefinition<>(
 *         new BeanReference<>(DummyBean.class, Optional.of(BeanStrategy.singleton), Optional.empty(), null),
 *         Optional.empty(), Set.of(), Set.of()
 *     )
 * );
 *
 * // Singleton returns same instance
 * Optional<DummyBean> bean1 = singletonFactory.supply();
 * Optional<DummyBean> bean2 = singletonFactory.supply();
 * assertSame(bean1.get(), bean2.get());
 *
 * // Create prototype bean factory
 * BeanFactory<DummyBean> prototypeFactory = new BeanFactory<>(
 *     new BeanDefinition<>(
 *         new BeanReference<>(DummyBean.class, Optional.of(BeanStrategy.prototype), Optional.empty(), null),
 *         Optional.empty(), Set.of(), Set.of()
 *     )
 * );
 *
 * // Prototype returns different instances
 * Optional<DummyBean> bean3 = prototypeFactory.supply();
 * Optional<DummyBean> bean4 = prototypeFactory.supply();
 * assertNotSame(bean3.get(), bean4.get());
 * }</pre>
 *
 * <h2>Usage Example: Bean Provider</h2>
 * Based on real test code from DiContextBuilderTest.java:
 * <pre>{@code
 * // Create context with multiple bean providers
 * IDiContext context = DiContext.builder()
 *     .withPackage("com.garganttua")
 *     .beanProvider(Predefined.BeanProviders.garganttua.toString(), new DummyBeanProviderBuilder())
 *         .up()
 *     .beanProvider("dummy", new DummyBeanProviderBuilder())
 *         .up()
 *     .build()
 *     .onInit()
 *     .onStart();
 *
 * // Verify providers are registered
 * assertEquals(2, context.getBeanProviders().size());
 * assertTrue(context.getBeanProvider(Predefined.BeanProviders.garganttua.toString()).isPresent());
 * }</pre>
 *
 * <h2>Usage Example: Bean Supplier</h2>
 * Based on real test code from BeanFactoryBuilderTest.java:
 * <pre>{@code
 * // Programmatically configure a bean supplier
 * String randomValue = UUID.randomUUID().toString();
 * IBeanFactoryBuilder<DummyBean> builder = new BeanFactoryBuilder<>(DummyBean.class);
 *
 * IBeanSupplier<DummyBean> beanSupplier = builder
 *     .strategy(BeanStrategy.singleton)
 *     .name("aBean")
 *     .qualifier(DummyBeanQualifier.class)
 *     .field(String.class).field("anotherValue")
 *         .withValue(FixedSupplierBuilder.of(randomValue))
 *         .up()
 *     .constructor()
 *         .withParam(FixedSupplierBuilder.of("constructedWithParameter"))
 *         .up()
 *     .postConstruction()
 *         .method("markPostConstruct")
 *         .withReturn(Void.class)
 *         .up()
 *     .build();
 *
 * Optional<DummyBean> bean = beanSupplier.supply();
 * assertEquals("constructedWithParameter", bean.get().getValue());
 * assertTrue(bean.get().isPostConstructCalled());
 * }</pre>
 *
 * <h2>Usage Example: Bean Query</h2>
 * Based on real test code from BeanQueryTest.java:
 * <pre>{@code
 * // Query by type
 * IBeanQueryBuilder<DummyBean> builder = BeanQuery.builder();
 * Optional<DummyBean> bean = builder.type(DummyBean.class).build().execute();
 * assertTrue(bean.isPresent());
 *
 * // Query by name
 * IBeanQueryBuilder<DummyBean> builder2 = BeanQuery.builder();
 * Optional<DummyBean> namedBean = builder2.name("dummyBeanForTest").build().execute();
 * assertTrue(namedBean.isPresent());
 *
 * // Query by type and name
 * IBeanQueryBuilder<DummyBean> builder3 = BeanQuery.builder();
 * Optional<DummyBean> exactBean = builder3
 *     .type(DummyBean.class)
 *     .name("dummyBeanForTest")
 *     .build()
 *     .execute();
 * assertTrue(exactBean.isPresent());
 *
 * // Query with non-existent name returns empty
 * IBeanQueryBuilder<DummyBean> builder4 = BeanQuery.builder();
 * Optional<DummyBean> notFound = builder4.name("toto").build().execute();
 * assertFalse(notFound.isPresent());
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
