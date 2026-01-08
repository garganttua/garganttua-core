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
 * Based on real test code from InjectionContextTest.java and InjectionContextBuilderTest.java:
 * <pre>{@code
 * // Basic context with package scanning and properties
 * InjectionContext.builder()
 *     .withPackage("com.garganttua")
 *     .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
 *         .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
 *         .up()
 *     .autoDetect(true)
 *     .build()
 *     .onInit()
 *     .onStart();
 *
 * // Context with custom bean and property providers
 * IInjectionContext context = InjectionContext.builder()
 *     .withPackage("com.garganttua")
 *     .beanProvider(Predefined.BeanProviders.garganttua.toString(), new DummyBeanProviderBuilder())
 *         .up()
 *     .beanProvider("dummy", new DummyBeanProviderBuilder())
 *         .up()
 *     .propertyProvider(Predefined.PropertyProviders.garganttua.toString(), new DummyPropertyProviderBuilder())
 *         .up()
 *     .propertyProvider("dummy", new DummyPropertyProviderBuilder())
 *         .up()
 *     .build()
 *     .onInit()
 *     .onStart();
 *
 * // Verify providers are registered
 * assertEquals(2, context.getBeanProviders().size());
 * assertEquals(2, context.getPropertyProviders().size());
 * }</pre>
 *
 * <h2>Usage Example: Bean Factory Configuration</h2>
 * Based on real test code from BeanFactoryBuilderTest.java:
 * <pre>{@code
 * // Programmatically configure a bean with full control
 * String randomValue = UUID.randomUUID().toString();
 * IBeanFactoryBuilder<DummyBean> builder = new BeanFactoryBuilder<>(DummyBean.class);
 *
 * IBeanSupplier<DummyBean> beanSupplier = builder
 *     // Set strategy and metadata
 *     .strategy(BeanStrategy.singleton)
 *     .name("aBean")
 *     .qualifier(DummyBeanQualifier.class)
 *
 *     // Configure field injection
 *     .field(String.class).field("anotherValue")
 *         .withValue(FixedSupplierBuilder.of(randomValue))
 *         .up()
 *
 *     // Configure constructor injection
 *     .constructor()
 *         .withParam(FixedSupplierBuilder.of("constructedWithParameter"))
 *         .up()
 *
 *     // Configure post-construct method
 *     .postConstruction()
 *         .method("markPostConstruct")
 *         .withReturn(Void.class)
 *         .up()
 *
 *     .build();
 *
 * // Supply the configured bean
 * Optional<DummyBean> bean = beanSupplier.supply();
 * assertEquals("constructedWithParameter", bean.get().getValue());
 * assertTrue(bean.get().isPostConstructCalled());
 * }</pre>
 *
 * <h2>Usage Example: Property Configuration</h2>
 * Based on real test code from InjectionContextTest.java:
 * <pre>{@code
 * // Configure properties with provider
 * InjectionContext.builder()
 *     .withPackage("com.garganttua")
 *     .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
 *         .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
 *         .up()
 *     .autoDetect(true)
 *     .build()
 *     .onInit()
 *     .onStart();
 *
 * // Retrieve property
 * Optional<String> property = Properties.property(String.class)
 *     .key("com.garganttua.dummyPropertyInConstructor")
 *     .build()
 *     .supply();
 * }</pre>
 *
 * <h2>Usage Example: Bean with Dependencies</h2>
 * Based on real test code from DummyBean.java:
 * <pre>{@code
 * // Bean with constructor dependency injection
 * @Singleton
 * @Named("dummyBeanForTest")
 * public class DummyBean {
 *     private String value;
 *     private AnotherDummyBean anotherBean;
 *
 *     @Inject
 *     public DummyBean(
 *         @Property("com.garganttua.dummyPropertyInConstructor") String value,
 *         @Prototype @Named("AnotherDummyBeanForTest") AnotherDummyBean anotherBean
 *     ) {
 *         this.value = value;
 *         this.anotherBean = anotherBean;
 *     }
 * }
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
 *   <li><b>InjectionContextBuilder</b> - Root builder
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
 * @see com.garganttua.core.injection.IInjectionContext
 */
package com.garganttua.core.injection.context.dsl;
