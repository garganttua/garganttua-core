/**
 * Method invocation, binding, and introspection utilities.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides utilities for invoking methods via reflection. It offers
 * type-safe method invocation with parameter binding, automatic accessibility handling,
 * and return type management.
 * </p>
 *
 * <h2>Usage Example: Method Invocation</h2>
 * <pre>{@code
 * public class UserService {
 *     private void updateUser(String userId, String name, int age) {
 *         // Implementation
 *     }
 *
 *     public String getWelcomeMessage(String name) {
 *         return "Welcome, " + name;
 *     }
 * }
 *
 * UserService service = new UserService();
 *
 * // Invoke private method
 * MethodInvoker updateInvoker = new MethodInvoker(service, "updateUser");
 * updateInvoker.withParameter(0, "user123");
 * updateInvoker.withParameter(1, "Alice");
 * updateInvoker.withParameter(2, 30);
 * updateInvoker.invoke();
 *
 * // Invoke public method with return value
 * MethodInvoker welcomeInvoker = new MethodInvoker(service, "getWelcomeMessage");
 * welcomeInvoker.withParameter(0, "Bob");
 * String message = welcomeInvoker.invoke();
 * }</pre>
 *
 * <h2>Usage Example: Method Queries</h2>
 * <pre>{@code
 * // Find all methods with @Provider annotation
 * List<Method> providerMethods = MethodQuery.findMethodsWithAnnotation(
 *     ConfigClass.class,
 *     Provider.class
 * );
 *
 * // Find all methods with @PostConstruct
 * List<Method> initMethods = MethodQuery.findMethodsWithAnnotation(
 *     UserService.class,
 *     PostConstruct.class
 * );
 *
 * // Find method by name
 * Method updateMethod = MethodQuery.findMethod(
 *     UserService.class,
 *     "updateUser",
 *     String.class, String.class, int.class
 * );
 * }</pre>
 *
 * <h2>Usage Example: Method Binding</h2>
 * <pre>{@code
 * // Create reusable method binder
 * MethodBinder binder = new MethodBinder(emailService, "sendEmail");
 *
 * // Bind parameters
 * binder.bindParameter(0, "recipient@example.com");
 * binder.bindParameter(1, "Subject Line");
 * binder.bindParameter(2, "Email body content");
 *
 * // Invoke multiple times with same binding
 * binder.invoke();
 * binder.invoke();  // Reuses parameter bindings
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe method invocation</li>
 *   <li>Automatic accessibility handling (private, protected, public)</li>
 *   <li>Parameter binding and type conversion</li>
 *   <li>Return value handling</li>
 *   <li>Method queries by name, signature, annotation</li>
 *   <li>Generic type preservation</li>
 *   <li>Exception wrapping and propagation</li>
 *   <li>Static method support</li>
 *   <li>Varargs support</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 * @see com.garganttua.core.reflection.binders.IMethodBinder
 */
package com.garganttua.core.reflection.methods;
