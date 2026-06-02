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
 * <h2>Usage Example: Resolving and Invoking a Method</h2>
 * <pre>{@code
 * public class UserService {
 *     public String getWelcomeMessage(String name) {
 *         return "Welcome, " + name;
 *     }
 * }
 *
 * IClass<UserService> owner = provider.getClass(UserService.class);
 *
 * // Resolve the method by name + signature
 * ResolvedMethod resolved = MethodResolver.methodByName(
 *     owner, provider, "getWelcomeMessage",
 *     provider.getClass(String.class), provider.getClass(String.class));
 *
 * // Invoke it against an instance
 * MethodInvoker<UserService, String> invoker = new MethodInvoker<>(resolved);
 * IMethodReturn<String> result = invoker.invoke(new UserService(), "Bob");
 * String message = result.single();
 * }</pre>
 *
 * <h2>Usage Example: Invoking Through a Field Path</h2>
 * <pre>{@code
 * // An ObjectAddress such as "inner.compute" resolves the leaf method `compute`
 * // reached by first reading the `inner` field; the invoker walks the path
 * // automatically and returns a MultipleMethodReturn when the path crosses a
 * // collection, array or map.
 * ResolvedMethod resolved = MethodResolver.methodByAddress(owner, provider,
 *     new ObjectAddress("inner.compute"));
 * MethodInvoker<?, ?> invoker = new MethodInvoker<>(resolved);
 * IMethodReturn<?> result = invoker.invoke(root);
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
 * @see com.garganttua.core.reflection.binders.MethodBinder
 */
package com.garganttua.core.reflection.methods;
