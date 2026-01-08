/**
 * Property resolution and management implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the implementation for property resolution, including property
 * providers, property suppliers, and the property management system. It handles property
 * placeholder resolution (${property.name}), default values, and multiple property sources.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code PropertyProvider} - Manages collections of property suppliers</li>
 *   <li>{@code PropertySupplier} - Provides individual property values</li>
 *   <li>{@code Properties} - Property utility and management class</li>
 * </ul>
 *
 * <h2>Property Resolution</h2>
 * <p>
 * Properties can come from multiple sources:
 * </p>
 * <ul>
 *   <li>Programmatically added properties</li>
 *   <li>Property files (application.properties, etc.)</li>
 *   <li>System properties</li>
 *   <li>Environment variables</li>
 *   <li>Custom property suppliers</li>
 * </ul>
 *
 * <h2>Usage Example: Property Provider</h2>
 * Based on real test code from InjectionContextTest.java and InjectionContextBuilderTest.java:
 * <pre>{@code
 * // Configure properties using predefined provider
 * String propertyValue = UUID.randomUUID().toString();
 *
 * InjectionContext.builder()
 *     .withPackage("com.garganttua")
 *     .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
 *         .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", propertyValue)
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
 *
 * assertTrue(property.isPresent());
 * assertEquals(propertyValue, property.get());
 * }</pre>
 *
 * <h2>Usage Example: Property in Bean Constructor</h2>
 * Based on real test code from DummyBean.java and InjectableElementResolverTest.java:
 * <pre>{@code
 * // Bean with property injection in constructor
 * @Singleton
 * @Named("dummyBeanForTest")
 * public class DummyBean {
 *     private String value;
 *
 *     @Inject
 *     public DummyBean(
 *         @Provider("garganttua")
 *         @Property("com.garganttua.dummyPropertyInConstructor") String value
 *     ) {
 *         this.value = value;
 *     }
 * }
 *
 * // Test property resolution
 * Constructor<DummyBean> ctor = DummyBean.class.getConstructor(String.class, AnotherDummyBean.class, DummyOtherBean.class);
 * Parameter[] params = ctor.getParameters();
 *
 * PropertyElementResolver propertyConstructor = new PropertyElementResolver();
 * Resolved resolved = propertyConstructor.resolve(params[0].getType(), params[0]);
 *
 * assertTrue(resolved.resolved());
 * assertEquals(String.class, resolved.elementSupplier().getSuppliedClass());
 * Optional<String> property = (Optional<String>) resolved.elementSupplier().build().supply();
 * assertEquals("propertyValue", property.get());
 * }</pre>
 *
 * <h2>Usage Example: Multiple Property Providers</h2>
 * Based on real test code from InjectionContextBuilderTest.java:
 * <pre>{@code
 * // Register multiple property providers
 * IInjectionContext context = InjectionContext.builder()
 *     .withPackage("com.garganttua")
 *     .propertyProvider(Predefined.PropertyProviders.garganttua.toString(), new DummyPropertyProviderBuilder())
 *         .up()
 *     .propertyProvider("dummy", new DummyPropertyProviderBuilder())
 *         .up()
 *     .build()
 *     .onInit()
 *     .onStart();
 *
 * // Verify providers are registered
 * assertEquals(2, context.getPropertyProviders().size());
 * assertTrue(context.getBeanProvider(Predefined.PropertyProviders.garganttua.toString()).isPresent());
 * }</pre>
 *
 * <h2>Usage Example: Property in Bean Lifecycle</h2>
 * Based on real test code from InjectionContextTest.java:
 * <pre>{@code
 * // Property is injected before post-construct
 * @Singleton
 * public class DummyBean {
 *     private String value;
 *     private boolean postConstructCalled = false;
 *
 *     @Inject
 *     public DummyBean(@Property("com.garganttua.dummyPropertyInConstructor") String value) {
 *         this.value = value;
 *     }
 *
 *     @PostConstruct
 *     public void markPostConstruct() {
 *         this.postConstructCalled = true;
 *     }
 * }
 *
 * // Verify property injection and post-construct execution
 * Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().supply();
 * assertEquals("propertyValue", bean.get().getValue()); // Property injected
 * assertTrue(bean.get().isPostConstructCalled()); // Post-construct executed
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Multiple property sources</li>
 *   <li>Placeholder resolution (${property.name})</li>
 *   <li>Default value support</li>
 *   <li>Property hierarchy (parent-child)</li>
 *   <li>Dynamic property suppliers</li>
 *   <li>Property file loading</li>
 *   <li>Environment variable integration</li>
 *   <li>System property integration</li>
 *   <li>Type conversion</li>
 *   <li>Nested placeholder resolution</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.properties.resolver} - Property value resolution strategies</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IPropertyProvider
 * @see com.garganttua.core.injection.context
 * @see com.garganttua.core.injection.annotations.Property
 */
package com.garganttua.core.injection.context.properties;
