/**
 * Core dependency injection framework implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the Garganttua dependency injection
 * framework. It implements the contracts defined in garganttua-commons and provides a
 * full-featured, lightweight IoC container with support for singleton and prototype scopes,
 * property injection, provider methods, and hierarchical contexts.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <p>
 * This package contains the core implementation of the DI framework, delegating
 * specific responsibilities to specialized sub-packages:
 * </p>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context} - Context implementation and management</li>
 *   <li>{@link com.garganttua.core.injection.context.beans} - Bean factory, provider, and supplier</li>
 *   <li>{@link com.garganttua.core.injection.context.properties} - Property resolution and management</li>
 *   <li>{@link com.garganttua.core.injection.context.validation} - Context and bean validation</li>
 *   <li>{@link com.garganttua.core.injection.context.resolver} - Injectable element resolution</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl} - Fluent builder implementations</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>JSR-330 compliant dependency injection</li>
 *   <li>Constructor, field, and setter injection</li>
 *   <li>Singleton and prototype bean scopes</li>
 *   <li>Property placeholder resolution (${property.name})</li>
 *   <li>Qualifier-based bean disambiguation</li>
 *   <li>Provider methods for complex bean creation</li>
 *   <li>Parent-child context hierarchies</li>
 *   <li>Lifecycle callbacks (post-construct, pre-destroy)</li>
 *   <li>Circular dependency detection</li>
 *   <li>Lazy bean initialization</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * Based on real test code from InjectionContextTest.java:
 * <pre>{@code
 * // Create and configure context with properties and auto-detection
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
 * // Query bean by type
 * Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().supply();
 *
 * if (bean.isPresent()) {
 *     System.out.println("Value: " + bean.get().getValue());
 *     System.out.println("Post-construct called: " + bean.get().isPostConstructCalled());
 * }
 *
 * // Query property
 * Optional<String> property = Properties.property(String.class)
 *     .key("com.garganttua.dummyPropertyInConstructor")
 *     .build()
 *     .supply();
 * }</pre>
 *
 * <h2>Architecture</h2>
 * <p>
 * The implementation follows a layered architecture:
 * </p>
 * <ul>
 *   <li><b>Context Layer</b> - Main DI context managing lifecycle</li>
 *   <li><b>Bean Layer</b> - Bean creation, caching, and scope management</li>
 *   <li><b>Property Layer</b> - Property resolution and placeholder substitution</li>
 *   <li><b>Resolver Layer</b> - Injectable element detection and resolution</li>
 *   <li><b>Validation Layer</b> - Configuration and dependency validation</li>
 *   <li><b>DSL Layer</b> - Fluent builder API implementation</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.context.InjectionContext
 * @see com.garganttua.core.injection.context.dsl
 * @see com.garganttua.core.injection.IInjectionContext
 */
package com.garganttua.core.injection;
