/**
 * Dependency injection framework contracts providing JSR-330 compatible IoC container interfaces.
 *
 * <h2>Overview</h2>
 * <p>
 * This package defines the core contracts for the Garganttua dependency injection framework.
 * It provides a lightweight, modular IoC container supporting singleton and prototype scopes,
 * property injection, provider methods, and hierarchical contexts.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.IDiContext} - Central DI container managing beans and properties</li>
 *   <li>{@link com.garganttua.core.injection.IBeanProvider} - Bean source abstraction for different strategies</li>
 *   <li>{@link com.garganttua.core.injection.IBeanFactory} - Factory for creating bean instances</li>
 *   <li>{@link com.garganttua.core.injection.IBeanQuery} - Query interface for bean lookup</li>
 *   <li>{@link com.garganttua.core.injection.IPropertyProvider} - Property source abstraction</li>
 * </ul>
 *
 * <h2>Bean Strategies</h2>
 * <p>
 * The framework supports multiple bean instantiation strategies via {@link com.garganttua.core.injection.BeanStrategy}:
 * </p>
 * <ul>
 *   <li><b>SINGLETON</b> - Single instance per context (default)</li>
 *   <li><b>PROTOTYPE</b> - New instance on each request</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Constructor, field, and setter injection</li>
 *   <li>Qualifier-based bean disambiguation</li>
 *   <li>Property placeholder resolution (${property.name})</li>
 *   <li>Parent-child context hierarchies</li>
 *   <li>Provider methods for complex bean creation</li>
 *   <li>Lifecycle callbacks (post-construct, pre-destroy)</li>
 * </ul>
 *
 * <h2>BeanReference - Bean Lookup DSL</h2>
 * <p>
 * {@link com.garganttua.core.injection.BeanReference} provides a powerful DSL for parsing
 * and matching beans in the dependency injection system. The syntax supports provider, type,
 * strategy, name, and qualifiers.
 * </p>
 *
 * <h3>Basic Parsing</h3>
 * <pre>{@code
 * // Parse by fully qualified class name
 * var ref1 = BeanReference.parse("java.lang.String");
 * // Type: String.class
 *
 * // With strategy (singleton or prototype)
 * var ref2 = BeanReference.parse("java.lang.String!singleton");
 * // Type: String.class, Strategy: singleton
 *
 * // With named bean
 * var ref3 = BeanReference.parse("java.lang.String#main");
 * // Type: String.class, Name: "main"
 *
 * // With provider prefix
 * var ref4 = BeanReference.parse("local::#Mail");
 * // Provider: "local", Name: "Mail"
 * }</pre>
 *
 * <h3>Advanced Parsing</h3>
 * <pre>{@code
 * // Full syntax: provider::class!strategy#name@qualifier
 * var ref = BeanReference.parse("local::java.lang.String!prototype#bean1");
 * // Provider: "local"
 * // Type: String.class
 * // Strategy: prototype
 * // Name: "bean1"
 *
 * // With qualifiers (fully qualified annotation class names)
 * var ref2 = BeanReference.parse("java.lang.String@com.example.Q1@com.example.Q2");
 * // Type: String.class
 * // Qualifiers: [Q1.class, Q2.class]
 *
 * // Complete example with all components
 * var ref3 = BeanReference.parse("remote::com.myapp.Service!singleton#primary@com.myapp.Production");
 * }</pre>
 *
 * <h3>Programmatic Creation and Matching</h3>
 * <pre>{@code
 * // Create references programmatically
 * BeanReference<String> ref1 = new BeanReference<>(
 *     String.class,
 *     Optional.of(BeanStrategy.singleton),
 *     Optional.of("myBean"),
 *     Set.of()
 * );
 *
 * BeanReference<String> ref2 = new BeanReference<>(
 *     String.class,
 *     Optional.of(BeanStrategy.singleton),
 *     Optional.of("myBean"),
 *     Set.of()
 * );
 *
 * // Check if references match (compares type, strategy, name, qualifiers)
 * boolean matches = ref1.matches(ref2); // true
 *
 * // Get effective name (explicit name or simple class name)
 * String name = ref1.effectiveName(); // "myBean"
 * }</pre>
 *
 * <h2>BeanDefinition - Bean Metadata</h2>
 * <p>
 * {@link com.garganttua.core.injection.BeanDefinition} encapsulates complete bean metadata
 * including reference, supplier, field binders, and method binders.
 * </p>
 *
 * <h3>Creating Bean Definitions</h3>
 * <pre>{@code
 * // Create bean reference
 * BeanReference<MyService> ref = new BeanReference<>(
 *     MyService.class,
 *     Optional.of(BeanStrategy.singleton),
 *     Optional.of("primaryService"),
 *     Set.of(PrimaryQualifier.class)
 * );
 *
 * // Create bean definition with all metadata
 * BeanDefinition<MyService> def = new BeanDefinition<>(
 *     ref,                           // Bean reference
 *     Optional.of(supplier),         // Optional supplier for bean creation
 *     Set.of(fieldBinder1, fieldBinder2), // Field injection binders
 *     Set.of(methodBinder1)          // Method injection binders
 * );
 *
 * // Access metadata
 * String effectiveName = def.reference().effectiveName(); // "primaryService"
 * Class<MyService> type = def.reference().type();         // MyService.class
 * BeanStrategy strategy = def.reference().strategy().get(); // singleton
 * }</pre>
 *
 * <h3>Equality and Matching</h3>
 * <pre>{@code
 * // Bean definitions are equal if their references match
 * BeanReference<MyService> ref1 = new BeanReference<>(
 *     MyService.class,
 *     Optional.of(BeanStrategy.singleton),
 *     Optional.of("myBean"),
 *     Set.of()
 * );
 *
 * BeanReference<MyService> ref2 = new BeanReference<>(
 *     MyService.class,
 *     Optional.of(BeanStrategy.singleton),
 *     Optional.of("myBean"),
 *     Set.of()
 * );
 *
 * BeanDefinition<MyService> def1 = new BeanDefinition<>(ref1, Optional.empty(), Set.of(), Set.of());
 * BeanDefinition<MyService> def2 = new BeanDefinition<>(ref2, Optional.empty(), Set.of(), Set.of());
 *
 * boolean equal = def1.equals(def2);         // true
 * boolean hashEqual = def1.hashCode() == def2.hashCode(); // true
 * boolean matches = def1.reference().matches(def2.reference()); // true
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.annotations} - Injection annotations (@Inject, @Qualifier, etc.)</li>
 *   <li>{@link com.garganttua.core.injection.context} - Context implementation support</li>
 *   <li>{@link com.garganttua.core.injection.context.dsl} - Fluent builder APIs for context configuration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IDiContext
 * @see com.garganttua.core.injection.IBeanProvider
 * @see com.garganttua.core.injection.context.dsl
 */
package com.garganttua.core.injection;
