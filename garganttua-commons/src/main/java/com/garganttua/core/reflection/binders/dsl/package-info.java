/**
 * Fluent builder APIs for constructing reflection-based binders.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides fluent DSL interfaces for building field, method, and constructor
 * binders. It offers a type-safe, readable way to configure complex reflection-based
 * binding scenarios with parameter mapping, value injection, and context awareness.
 * </p>
 *
 * <h2>Core Builder Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder} - Builds field binders</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder} - Builds method binders</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder} - Builds constructor binders</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IExecutableBinderBuilder} - Common builder for executables</li>
 * </ul>
 *
 * <h2>Configuration Builders</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IParametrableBuilder} - Configures method/constructor parameters</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl.IValuableBuilder} - Configures value binding strategies</li>
 * </ul>
 *
 * <h2>Usage Example: Field Binder</h2>
 * <pre>{@code
 * // Build a field binder with fluent API
 * IFieldBinder binder = new FieldBinderBuilder()
 *     .target(UserService.class)
 *     .field("apiUrl")
 *     .value("https://api.example.com")
 *     .build();
 *
 * // Apply binding
 * UserService service = new UserService();
 * binder.inject(service);
 * }</pre>
 *
 * <h2>Usage Example: Constructor Binder</h2>
 * <pre>{@code
 * // Build a constructor binder
 * IConstructorBinder binder = new ConstructorBinderBuilder()
 *     .target(DatabaseService.class)
 *     .parameter(0)
 *         .value("jdbc:mysql://localhost:3306/mydb")
 *         .done()
 *     .parameter(1)
 *         .property("db.username")
 *         .done()
 *     .parameter(2)
 *         .property("db.password")
 *         .done()
 *     .build();
 *
 * // Create instance
 * DatabaseService service = binder.newInstance();
 * }</pre>
 *
 * <h2>Usage Example: Method Binder</h2>
 * <pre>{@code
 * EmailService emailService = new EmailService();
 *
 * // Build a method binder
 * IMethodBinder binder = new MethodBinderBuilder()
 *     .target(emailService)
 *     .method("sendEmail")
 *     .parameter(0)
 *         .value("user@example.com")
 *         .done()
 *     .parameter(1)
 *         .value("Welcome!")
 *         .done()
 *     .parameter(2)
 *         .template("Hello ${user.name}, welcome to our platform!")
 *         .done()
 *     .build();
 *
 * // Invoke method
 * binder.invoke();
 * }</pre>
 *
 * <h2>Usage Example: Context-Aware Binding</h2>
 * <pre>{@code
 * // Build field binder with DI context integration
 * IFieldBinder binder = new FieldBinderBuilder()
 *     .target(UserService.class)
 *     .field("repository")
 *     .bean(UserRepository.class)
 *     .qualifier("primary")
 *     .build();
 *
 * // Build with property resolution
 * IFieldBinder configBinder = new FieldBinderBuilder()
 *     .target(UserService.class)
 *     .field("maxRetries")
 *     .property("service.max.retries")
 *     .defaultValue(3)
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Complex Parameter Binding</h2>
 * <pre>{@code
 * // Build method with mixed parameter sources
 * IMethodBinder binder = new MethodBinderBuilder()
 *     .target(orderService)
 *     .method("processOrder")
 *     .parameter(0)  // From bean
 *         .bean(OrderRepository.class)
 *         .done()
 *     .parameter(1)  // From property
 *         .property("order.processing.timeout")
 *         .done()
 *     .parameter(2)  // Fixed value
 *         .value("STANDARD")
 *         .done()
 *     .parameter(3)  // From supplier
 *         .supplier(() -> UUID.randomUUID().toString())
 *         .done()
 *     .build();
 * }</pre>
 *
 * <h2>Value Binding Strategies</h2>
 * <p>
 * The {@link com.garganttua.core.reflection.binders.dsl.IValuableBuilder} supports multiple strategies:
 * </p>
 * <ul>
 *   <li><b>value(Object)</b> - Direct value assignment</li>
 *   <li><b>property(String)</b> - Property placeholder resolution</li>
 *   <li><b>bean(Class)</b> - Bean lookup from DI context</li>
 *   <li><b>supplier(Supplier)</b> - Dynamic value from supplier</li>
 *   <li><b>template(String)</b> - String template with variable substitution</li>
 *   <li><b>nullValue()</b> - Explicit null assignment</li>
 *   <li><b>defaultValue(Object)</b> - Fallback value</li>
 * </ul>
 *
 * <h2>Parameter Configuration</h2>
 * <p>
 * The {@link com.garganttua.core.reflection.binders.dsl.IParametrableBuilder} enables:
 * </p>
 * <ul>
 *   <li>Index-based parameter selection</li>
 *   <li>Type-safe value binding</li>
 *   <li>Qualifier support for bean injection</li>
 *   <li>Optional parameter handling</li>
 *   <li>Default value specification</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe configuration</li>
 *   <li>Multiple value sources</li>
 *   <li>Property placeholder support</li>
 *   <li>Bean injection integration</li>
 *   <li>Template string support</li>
 *   <li>Default value handling</li>
 *   <li>Null safety</li>
 *   <li>Clear error messages</li>
 * </ul>
 *
 * <h2>Builder Pattern</h2>
 * <p>
 * All builders follow these conventions:
 * </p>
 * <ul>
 *   <li>Method chaining for fluent configuration</li>
 *   <li>{@code done()} returns to parent builder</li>
 *   <li>{@code build()} creates the binder</li>
 *   <li>Type parameters preserve compile-time safety</li>
 *   <li>Clear builder hierarchy</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.binders
 * @see com.garganttua.core.reflection.binders.dsl.IFieldBinderBuilder
 * @see com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder
 * @see com.garganttua.core.reflection.binders.dsl.IParametrableBuilder
 * @see com.garganttua.core.dsl.IAutomaticBuilder
 */
package com.garganttua.core.reflection.binders.dsl;
