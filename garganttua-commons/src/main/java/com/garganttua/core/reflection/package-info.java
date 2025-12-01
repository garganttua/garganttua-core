/**
 * Advanced reflection utilities for type-safe field, method, and constructor binding.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides advanced reflection capabilities with type-safe interfaces for binding
 * to fields, methods, and constructors. It supports annotation scanning, object graph navigation
 * via dot-notation paths, and dynamic invocation with parameter injection.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.IBinder} - Base interface for reflection binding</li>
 *   <li>{@link com.garganttua.core.reflection.IFieldBinder} - Type-safe field access and modification</li>
 *   <li>{@link com.garganttua.core.reflection.IMethodBinder} - Type-safe method invocation</li>
 *   <li>{@link com.garganttua.core.reflection.IConstructorBinder} - Type-safe constructor invocation</li>
 *   <li>{@link com.garganttua.core.reflection.IAnnotationScanner} - Annotation discovery</li>
 * </ul>
 *
 * <h2>Field Binding</h2>
 * <pre>{@code
 * // Bind to field
 * IFieldBinder<User, String> nameBinder = FieldBinderBuilder
 *     .create(User.class, "name", String.class)
 *     .build();
 *
 * User user = new User();
 *
 * // Set value
 * nameBinder.set(user, "John Doe");
 *
 * // Get value
 * String name = nameBinder.get(user); // "John Doe"
 *
 * // Nested path binding
 * IFieldBinder<Order, String> cityBinder = FieldBinderBuilder
 *     .create(Order.class, "customer.address.city", String.class)
 *     .build();
 *
 * String city = cityBinder.get(order);
 * }</pre>
 *
 * <h2>Method Binding</h2>
 * <pre>{@code
 * // Bind to method
 * IMethodBinder<UserService, Boolean, User, String> methodBinder = MethodBinderBuilder
 *     .create(UserService.class, "authenticate", Boolean.class)
 *     .param(User.class)
 *     .param(String.class)
 *     .build();
 *
 * UserService service = new UserService();
 * User user = new User("john");
 * String password = "secret";
 *
 * // Invoke method
 * Boolean result = methodBinder.invoke(service, user, password);
 *
 * // Invoke with supplier
 * Boolean result2 = methodBinder.invoke(service, () -> user, () -> password);
 * }</pre>
 *
 * <h2>Constructor Binding</h2>
 * <pre>{@code
 * // Bind to constructor
 * IConstructorBinder<User, String, Integer> ctorBinder = ConstructorBinderBuilder
 *     .create(User.class)
 *     .param(String.class)
 *     .param(Integer.class)
 *     .build();
 *
 * // Create instance
 * User user = ctorBinder.newInstance("Alice", 30);
 * }</pre>
 *
 * <h2>Annotation Scanning</h2>
 * <pre>{@code
 * IAnnotationScanner scanner = new ReflectionAnnotationScanner("com.example");
 *
 * // Find all classes with annotation
 * Set<Class<?>> entities = scanner.getClassesAnnotatedWith(Entity.class);
 *
 * // Find all methods with annotation
 * Set<Method> endpoints = scanner.getMethodsAnnotatedWith(Endpoint.class);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe reflection with generics</li>
 *   <li>Dot-notation path support for nested objects</li>
 *   <li>Annotation scanning with caching</li>
 *   <li>Private field/method access</li>
 *   <li>Supplier-based parameter injection</li>
 *   <li>Performance optimized (method handles)</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders} - Binder implementations</li>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl} - Fluent builder APIs for binder creation</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.IBinder
 * @see com.garganttua.core.reflection.IFieldBinder
 * @see com.garganttua.core.reflection.IMethodBinder
 */
package com.garganttua.core.reflection;
