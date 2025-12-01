/**
 * Reflection-based binding implementations for fields, methods, and constructors.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of binder interfaces defined in
 * garganttua-commons. It enables type-safe, declarative configuration of object
 * construction and initialization through reflection.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code FieldBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IFieldBinder}</li>
 *   <li>{@code MethodBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IMethodBinder}</li>
 *   <li>{@code ConstructorBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IConstructorBinder}</li>
 *   <li>{@code ExecutableBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IExecutableBinder}</li>
 * </ul>
 *
 * <h2>Usage Example: Field Binder Implementation</h2>
 * <pre>{@code
 * // Create field binder
 * IFieldBinder binder = new FieldBinder(UserService.class, "apiUrl");
 *
 * // Bind value
 * binder.bind("https://api.example.com");
 *
 * // Apply to instance
 * UserService service = new UserService();
 * binder.inject(service);
 * }</pre>
 *
 * <h2>Usage Example: Constructor Binder Implementation</h2>
 * <pre>{@code
 * // Create constructor binder
 * IConstructorBinder binder = new ConstructorBinder(DatabaseService.class);
 *
 * // Bind parameters
 * binder.bindParameter(0, "jdbc:mysql://localhost:3306/mydb");
 * binder.bindParameter(1, "admin");
 * binder.bindParameter(2, "secret");
 *
 * // Create instance
 * DatabaseService service = binder.newInstance();
 * }</pre>
 *
 * <h2>Usage Example: Method Binder Implementation</h2>
 * <pre>{@code
 * EmailService emailService = new EmailService();
 *
 * // Create method binder
 * IMethodBinder binder = new MethodBinder(emailService, "sendEmail");
 *
 * // Bind parameters
 * binder.bindParameter(0, "user@example.com");
 * binder.bindParameter(1, "Welcome!");
 * binder.bindParameter(2, "Thank you for joining.");
 *
 * // Invoke method
 * binder.invoke();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe parameter binding</li>
 *   <li>Support for primitive and object types</li>
 *   <li>Automatic type conversion</li>
 *   <li>Accessibility management</li>
 *   <li>Null safety</li>
 *   <li>Exception handling and wrapping</li>
 *   <li>Generic type preservation</li>
 *   <li>Performance optimization</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl} - Builder implementations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.binders
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.binders;
