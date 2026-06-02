/**
 * Constructor access, invocation, and object instantiation utilities.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides utilities for instantiating objects via reflection using
 * constructors. It offers type-safe constructor invocation with parameter binding,
 * automatic accessibility handling, and support for complex object creation scenarios.
 * </p>
 *
 * <h2>Usage Example: Constructor Resolution and Invocation</h2>
 * <pre>{@code
 * IClass<User> userType = IClass.getClass(User.class);
 *
 * // Resolve a constructor by parameter types, then invoke it
 * ResolvedConstructor<User> resolved = ConstructorResolver.constructorByParameterTypes(
 *     userType, provider, IClass.getClass(String.class), IClass.getClass(int.class));
 *
 * ConstructorInvoker<User> invoker = new ConstructorInvoker<>(resolved);
 * IMethodReturn<User> result = invoker.newInstance("Alice", 30);
 * User user = result.single();
 * }</pre>
 *
 * <h2>Usage Example: Constructor Resolution Variants</h2>
 * <pre>{@code
 * // Resolve the no-arg constructor
 * ResolvedConstructor<User> def = ConstructorResolver.defaultConstructor(userType, provider);
 *
 * // Resolve the constructor annotated with a given annotation
 * ResolvedConstructor<User> injected = ConstructorResolver.constructorByAnnotation(
 *     userType, provider, IClass.getClass(Inject.class));
 *
 * // Enumerate all declared constructors
 * List<ResolvedConstructor<User>> all = ConstructorResolver.allConstructors(userType, provider);
 * }</pre>
 *
 * <h2>Usage Example: Forcing Access to a Private Constructor</h2>
 * <pre>{@code
 * // The second argument forces accessibility for non-public constructors
 * ConstructorInvoker<User> invoker = new ConstructorInvoker<>(resolved, true);
 * User user = invoker.newInstance("Bob", 25, "bob@example.com").single();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe constructor invocation</li>
 *   <li>Automatic accessibility handling (private, protected, public)</li>
 *   <li>Parameter binding and type conversion</li>
 *   <li>Constructor queries by signature, annotation</li>
 *   <li>Default constructor support</li>
 *   <li>Generic type preservation</li>
 *   <li>Exception wrapping</li>
 *   <li>Support for inner classes</li>
 *   <li>Varargs support</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 * @see com.garganttua.core.reflection.binders.IConstructorBinder
 */
package com.garganttua.core.reflection.constructors;
