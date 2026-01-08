/**
 * Reflection-based binding abstractions for fields, methods, and constructors.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides high-level abstractions for binding values to reflective elements
 * (fields, methods, constructors). It enables type-safe, declarative configuration of
 * object construction and initialization through reflection while hiding low-level
 * reflection API complexity.
 * </p>
 *
 * <h2>Core Binder Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.IFieldBinder} - Binds values to fields</li>
 *   <li>{@link com.garganttua.core.reflection.binders.IMethodBinder} - Binds parameters to methods</li>
 *   <li>{@link com.garganttua.core.reflection.binders.IConstructorBinder} - Binds parameters to constructors</li>
 *   <li>{@link com.garganttua.core.reflection.binders.IExecutableBinder} - Common interface for methods and constructors</li>
 * </ul>
 *
 * <h2>Contextual Binders</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.IContextualFieldBinder} - Field binding with context awareness</li>
 *   <li>{@link com.garganttua.core.reflection.binders.IContextualMethodBinder} - Method binding with context</li>
 *   <li>{@link com.garganttua.core.reflection.binders.IContextualConstructorBinder} - Constructor binding with context</li>
 *   <li>{@link com.garganttua.core.reflection.binders.IContextualExecutableBinder} - Executable binding with context</li>
 * </ul>
 *
 * <h2>Dependency Marker</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.Dependent} - Marks binders with dependencies</li>
 * </ul>
 *
 * <h2>Usage Example: Field Binding</h2>
 * <pre>{@code
 * public class UserService {
 *     private String apiUrl;
 *     private int timeout;
 * }
 *
 * // Create binders
 * IFieldBinder urlBinder = new FieldBinder(UserService.class, "apiUrl")
 *     .bind("https://api.example.com");
 *
 * IFieldBinder timeoutBinder = new FieldBinder(UserService.class, "timeout")
 *     .bind(30);
 *
 * // Apply bindings
 * UserService service = new UserService();
 * urlBinder.inject(service);
 * timeoutBinder.inject(service);
 * }</pre>
 *
 * <h2>Usage Example: Constructor Binding</h2>
 * <pre>{@code
 * public class DatabaseService {
 *     public DatabaseService(String url, String user, String password) {
 *         // ...
 *     }
 * }
 *
 * // Create constructor binder
 * IConstructorBinder binder = new ConstructorBinder(DatabaseService.class)
 *     .bindParameter(0, "jdbc:mysql://localhost:3306/mydb")
 *     .bindParameter(1, "admin")
 *     .bindParameter(2, "secret");
 *
 * // Create instance
 * DatabaseService service = binder.newInstance();
 * }</pre>
 *
 * <h2>Usage Example: Method Binding</h2>
 * <pre>{@code
 * public class EmailService {
 *     public void sendEmail(String to, String subject, String body) {
 *         // ...
 *     }
 * }
 *
 * // Create method binder
 * IMethodBinder binder = new MethodBinder(emailService, "sendEmail")
 *     .bindParameter(0, "user@example.com")
 *     .bindParameter(1, "Welcome!")
 *     .bindParameter(2, "Thank you for joining.");
 *
 * // Invoke method
 * binder.invoke();
 * }</pre>
 *
 * <h2>Usage Example: Contextual Binding</h2>
 * <pre>{@code
 * // Contextual field binder with dependency injection context
 * IContextualFieldBinder<IInjectionContext> binder =
 *     new ContextualFieldBinder<>(UserService.class, "repository")
 *         .bindFromContext(ctx -> ctx.getBean(UserRepository.class));
 *
 * // Apply with context
 * UserService service = new UserService();
 * binder.inject(service, injectionContext);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe parameter binding</li>
 *   <li>Support for primitive and object types</li>
 *   <li>Automatic type conversion</li>
 *   <li>Context-aware binding strategies</li>
 *   <li>Dependency tracking</li>
 *   <li>Null safety</li>
 *   <li>Exception handling</li>
 *   <li>Accessibility management</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * Binders are used extensively by:
 * </p>
 * <ul>
 *   <li>Dependency injection framework for bean instantiation</li>
 *   <li>Runtime execution framework for method invocation</li>
 *   <li>Object mapping for field-to-field copying</li>
 *   <li>Configuration binding</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.binders.dsl
 * @see com.garganttua.core.reflection
 * @see com.garganttua.core.injection
 */
package com.garganttua.core.reflection.binders;
