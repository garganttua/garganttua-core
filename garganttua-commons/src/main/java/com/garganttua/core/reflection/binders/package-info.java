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
 * // Read/write a field via a binder configured through the DSL
 * User user = new User();
 * IFieldBinder<User, String> nameBinder = FieldBinder
 *     .forInstance(user)
 *     .field("name")
 *     .withValue("Alice")
 *     .build();
 *
 * nameBinder.setValue();          // user.name = "Alice"
 * String name = nameBinder.getValue();
 * }</pre>
 *
 * <h2>Usage Example: Constructor Binding</h2>
 * <pre>{@code
 * // Instantiate via a resolved constructor
 * IConstructorBinder<Database> binder = ConstructorBinder
 *     .forClass(Database.class)
 *     .withParam("jdbc:mysql://localhost:3306/mydb")
 *     .withParam(3306)
 *     .build();
 *
 * Optional<IMethodReturn<Database>> instance = binder.execute();
 * }</pre>
 *
 * <h2>Usage Example: Method Binding</h2>
 * <pre>{@code
 * // Invoke an instance method
 * StringBuilder target = new StringBuilder("Hello");
 * IMethodBinder<StringBuilder> binder = MethodBinder
 *     .forInstance(target)
 *     .method("append")
 *     .withParam(" World")
 *     .build();
 *
 * Optional<IMethodReturn<StringBuilder>> result = binder.execute();
 * }</pre>
 *
 * <h2>Usage Example: Contextual Binding</h2>
 * <pre>{@code
 * // A contextual field binder resolves its value from a runtime context
 * IContextualFieldBinder<UserService, UserRepository, InjectionContext, InjectionContext> binder =
 *     ContextualFieldBinder
 *         .forClass(UserService.class)
 *         .field("repository")
 *         .build();
 *
 * // Apply with context
 * binder.setValue(injectionContext, injectionContext);
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
