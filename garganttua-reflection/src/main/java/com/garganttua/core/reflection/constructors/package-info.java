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
 * <h2>Usage Example: Constructor Invocation</h2>
 * <pre>{@code
 * public class User {
 *     private String name;
 *     private int age;
 *     private String email;
 *
 *     public User(String name, int age) {
 *         this.name = name;
 *         this.age = age;
 *     }
 *
 *     private User(String name, int age, String email) {
 *         this.name = name;
 *         this.age = age;
 *         this.email = email;
 *     }
 * }
 *
 * // Invoke public constructor
 * ConstructorAccessor<User> publicConstructor =
 *     new ConstructorAccessor<>(User.class, String.class, int.class);
 *
 * publicConstructor.withParameter(0, "Alice");
 * publicConstructor.withParameter(1, 30);
 * User user1 = publicConstructor.newInstance();
 *
 * // Invoke private constructor
 * ConstructorAccessor<User> privateConstructor =
 *     new ConstructorAccessor<>(User.class, String.class, int.class, String.class);
 *
 * privateConstructor.withParameter(0, "Bob");
 * privateConstructor.withParameter(1, 25);
 * privateConstructor.withParameter(2, "bob@example.com");
 * User user2 = privateConstructor.newInstance();
 * }</pre>
 *
 * <h2>Usage Example: Constructor Queries</h2>
 * <pre>{@code
 * // Find all constructors
 * List<Constructor<?>> constructors = ConstructorQuery.findAllConstructors(
 *     User.class
 * );
 *
 * // Find constructor with @Inject annotation
 * Constructor<?> injectConstructor = ConstructorQuery.findInjectableConstructor(
 *     UserService.class
 * );
 *
 * // Find constructor by parameter types
 * Constructor<User> constructor = ConstructorQuery.findConstructor(
 *     User.class,
 *     String.class, int.class
 * );
 * }</pre>
 *
 * <h2>Usage Example: Constructor Binding</h2>
 * <pre>{@code
 * // Create reusable constructor binder
 * ConstructorBinder<DataSource> binder =
 *     new ConstructorBinder<>(HikariDataSource.class);
 *
 * // Bind parameters
 * binder.bindParameter(0, "jdbc:mysql://localhost:3306/mydb");
 * binder.bindParameter(1, "admin");
 * binder.bindParameter(2, "password");
 *
 * // Create multiple instances with same parameters
 * DataSource ds1 = binder.newInstance();
 * DataSource ds2 = binder.newInstance();
 * }</pre>
 *
 * <h2>Usage Example: Default Constructor</h2>
 * <pre>{@code
 * // Invoke default (no-arg) constructor
 * ConstructorAccessor<User> defaultConstructor =
 *     new ConstructorAccessor<>(User.class);
 *
 * User user = defaultConstructor.newInstance();
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
