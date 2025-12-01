/**
 * Fluent builder API implementations for constructing reflection-based binders.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of the fluent DSL interfaces defined
 * in garganttua-commons for building field, method, and constructor binders. It implements
 * the builder pattern to provide a type-safe, readable API for configuring reflection-based
 * binding scenarios.
 * </p>
 *
 * <h2>Implementation Classes</h2>
 * <p>
 * This package contains implementations of the builder interfaces from
 * {@link com.garganttua.core.reflection.binders.dsl} (commons package).
 * </p>
 *
 * <h2>Usage Example: Field Binder Builder Implementation</h2>
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
 * <h2>Usage Example: Constructor Binder Builder Implementation</h2>
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
 * <h2>Usage Example: Method Binder Builder Implementation</h2>
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
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe configuration</li>
 *   <li>Multiple value sources (direct, property, bean, supplier)</li>
 *   <li>Property placeholder support</li>
 *   <li>Bean injection integration</li>
 *   <li>Template string support</li>
 *   <li>Default value handling</li>
 *   <li>Null safety</li>
 *   <li>Clear error messages</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.binders.dsl
 * @see com.garganttua.core.reflection.binders
 */
package com.garganttua.core.reflection.binders.dsl;
