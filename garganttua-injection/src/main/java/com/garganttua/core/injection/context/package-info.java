/**
 * Dependency injection context implementation and management.
 *
 * <h2>Overview</h2>
 * <p>
 * This package contains the core implementation of the DI context ({@code DiContext}),
 * which orchestrates bean lifecycle, property resolution, dependency injection, and
 * context hierarchy management. It serves as the central coordinator for all DI operations.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code DiContext} - Main DI context implementation managing beans and properties</li>
 *   <li>{@code Predefined} - Predefined bean and property constants</li>
 * </ul>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Bean lifecycle management (creation, initialization, destruction)</li>
 *   <li>Dependency graph resolution and injection</li>
 *   <li>Property placeholder resolution</li>
 *   <li>Scope management (singleton vs prototype)</li>
 *   <li>Context hierarchy (parent-child relationships)</li>
 *   <li>Circular dependency detection</li>
 *   <li>Post-construct and pre-destroy callback execution</li>
 * </ul>
 *
 * <h2>Context Lifecycle</h2>
 * Based on real test code from DiContextTest.java:
 * <pre>{@code
 * // 1. Create context with package scanning and properties
 * DiContext.builder()
 *     .withPackage("com.garganttua")
 *     .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
 *         .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
 *         .up()
 *     .autoDetect(true)
 *     .build()
 *     .onInit()
 *     .onStart();
 *
 * // 2. Use context - query beans
 * Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().supply();
 * if (bean.isPresent()) {
 *     System.out.println("Value: " + bean.get().getValue());
 *     System.out.println("Post-construct called: " + bean.get().isPostConstructCalled());
 * }
 * }</pre>
 *
 * <h2>Bean Resolution</h2>
 * Based on real test code from BeanQueryTest.java:
 * <pre>{@code
 * // Query by type
 * IBeanQueryBuilder<DummyBean> builder = BeanQuery.builder();
 * Optional<DummyBean> bean = builder.type(DummyBean.class).build().execute();
 *
 * // Query by name
 * IBeanQueryBuilder<DummyBean> builder2 = BeanQuery.builder();
 * Optional<DummyBean> namedBean = builder2.name("dummyBeanForTest").build().execute();
 *
 * // Query by type and name
 * IBeanQueryBuilder<DummyBean> builder3 = BeanQuery.builder();
 * Optional<DummyBean> exactBean = builder3
 *     .type(DummyBean.class)
 *     .name("dummyBeanForTest")
 *     .build()
 *     .execute();
 * }</pre>
 *
 * <h2>Property Resolution</h2>
 * Based on real test code from DiContextTest.java:
 * <pre>{@code
 * // Configure property
 * DiContext.builder()
 *     .withPackage("com.garganttua")
 *     .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
 *         .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
 *         .up()
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
 * assertEquals("propertyValue", property.get());
 * }</pre>
 *
 * <h2>Context Hierarchy</h2>
 * Note: Working examples are not present in current test suite.
 * <pre>{@code
 * // Create parent context
 * IDiContext parent = DiContext.builder()
 *     .withPackage("com.myapp.core")
 *     .build();
 *
 * // Create child context
 * IDiContext child = parent.newChildContext(CustomChildContext.class, "module1");
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.beans} - Bean management implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.properties} - Property resolution implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl} - Context builder implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.validation} - Validation implementation</li>
 *   <li>{@link com.garganttua.core.injection.context.resolver} - Element resolver implementation</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IDiContext
 * @see com.garganttua.core.injection.context.beans
 * @see com.garganttua.core.injection.context.dsl
 */
package com.garganttua.core.injection.context;
