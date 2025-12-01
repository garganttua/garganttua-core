/**
 * Advanced reflection utilities and object manipulation implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of reflection utilities for the
 * Garganttua framework. It implements high-level abstractions over Java's reflection
 * API, providing type-safe field access, method invocation, constructor binding, and
 * object introspection capabilities.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code ObjectAccessor} - Unified object access and manipulation interface</li>
 * </ul>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.fields} - Field access and manipulation</li>
 *   <li>{@link com.garganttua.core.reflection.methods} - Method invocation and binding</li>
 *   <li>{@link com.garganttua.core.reflection.constructors} - Constructor access and instantiation</li>
 *   <li>{@link com.garganttua.core.reflection.query} - Reflection-based queries</li>
 *   <li>{@link com.garganttua.core.reflection.binders} - Binder implementations</li>
 *   <li>{@link com.garganttua.core.reflection.utils} - Reflection utility classes</li>
 * </ul>
 *
 * <h2>Usage Example: Object Accessor</h2>
 * <pre>{@code
 * public class User {
 *     private String name;
 *     private int age;
 *
 *     public void greet(String message) {
 *         System.out.println(message + ", " + name);
 *     }
 * }
 *
 * // Create object accessor
 * User user = new User();
 * ObjectAccessor accessor = new ObjectAccessor(user);
 *
 * // Set field values
 * accessor.setField("name", "Alice");
 * accessor.setField("age", 30);
 *
 * // Get field values
 * String name = accessor.getField("name");
 * int age = accessor.getField("age");
 *
 * // Invoke method
 * accessor.invokeMethod("greet", "Hello");
 * }</pre>
 *
 * <h2>Usage Example: Field Access</h2>
 * <pre>{@code
 * // Direct field access
 * FieldAccessor fieldAccessor = new FieldAccessor(User.class, "email");
 * fieldAccessor.set(user, "alice@example.com");
 * String email = fieldAccessor.get(user);
 *
 * // Field query
 * List<Field> annotatedFields = FieldQuery.findFieldsWithAnnotation(
 *     User.class,
 *     Inject.class
 * );
 * }</pre>
 *
 * <h2>Usage Example: Method Invocation</h2>
 * <pre>{@code
 * // Method invoker
 * MethodInvoker invoker = new MethodInvoker(user, "updateProfile");
 * invoker.withParameter(0, "New Name");
 * invoker.withParameter(1, "new.email@example.com");
 * Object result = invoker.invoke();
 *
 * // Method query
 * List<Method> providers = MethodQuery.findMethodsWithAnnotation(
 *     ConfigClass.class,
 *     Provider.class
 * );
 * }</pre>
 *
 * <h2>Usage Example: Constructor Access</h2>
 * <pre>{@code
 * // Constructor invocation
 * ConstructorAccessor<User> constructor =
 *     new ConstructorAccessor<>(User.class);
 *
 * constructor.withParameter(0, "Alice");
 * constructor.withParameter(1, 30);
 *
 * User user = constructor.newInstance();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe field access (get/set)</li>
 *   <li>Method invocation with parameter binding</li>
 *   <li>Constructor invocation with parameter binding</li>
 *   <li>Annotation-based queries</li>
 *   <li>Access modifier handling (private, protected, public)</li>
 *   <li>Generic type preservation</li>
 *   <li>Automatic type conversion</li>
 *   <li>Null safety</li>
 *   <li>Exception wrapping</li>
 *   <li>Performance optimization through caching</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * This reflection implementation is used by:
 * </p>
 * <ul>
 *   <li>Dependency injection framework for bean instantiation and injection</li>
 *   <li>Runtime execution framework for method invocation</li>
 *   <li>Object mapper for field-to-field copying</li>
 *   <li>Property binding for configuration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.fields
 * @see com.garganttua.core.reflection.methods
 * @see com.garganttua.core.reflection.constructors
 * @see com.garganttua.core.reflection.binders
 */
package com.garganttua.core.reflection;
