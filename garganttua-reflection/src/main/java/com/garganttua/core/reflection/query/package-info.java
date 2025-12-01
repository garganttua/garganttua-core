/**
 * Reflection-based query utilities for discovering class members.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides query utilities for discovering class members (fields, methods,
 * constructors) based on various criteria such as annotations, types, names, and modifiers.
 * It simplifies reflection-based introspection and metadata collection.
 * </p>
 *
 * <h2>Usage Example: Field Queries</h2>
 * <pre>{@code
 * // Find all fields with @Inject annotation
 * List<Field> injectableFields = ReflectionQuery.findFields(
 *     UserService.class,
 *     field -> field.isAnnotationPresent(Inject.class)
 * );
 *
 * // Find all private fields
 * List<Field> privateFields = ReflectionQuery.findFields(
 *     UserService.class,
 *     field -> Modifier.isPrivate(field.getModifiers())
 * );
 *
 * // Find field by name
 * Optional<Field> emailField = ReflectionQuery.findField(
 *     User.class,
 *     "email"
 * );
 * }</pre>
 *
 * <h2>Usage Example: Method Queries</h2>
 * <pre>{@code
 * // Find all methods with @Provider annotation
 * List<Method> providers = ReflectionQuery.findMethods(
 *     ConfigClass.class,
 *     method -> method.isAnnotationPresent(Provider.class)
 * );
 *
 * // Find all public methods
 * List<Method> publicMethods = ReflectionQuery.findMethods(
 *     UserService.class,
 *     method -> Modifier.isPublic(method.getModifiers())
 * );
 *
 * // Find methods by name
 * List<Method> getMethods = ReflectionQuery.findMethods(
 *     User.class,
 *     method -> method.getName().startsWith("get")
 * );
 * }</pre>
 *
 * <h2>Usage Example: Constructor Queries</h2>
 * <pre>{@code
 * // Find all public constructors
 * List<Constructor<?>> publicConstructors = ReflectionQuery.findConstructors(
 *     User.class,
 *     ctor -> Modifier.isPublic(ctor.getModifiers())
 * );
 *
 * // Find constructor with @Inject
 * Optional<Constructor<?>> injectConstructor = ReflectionQuery.findConstructor(
 *     UserService.class,
 *     ctor -> ctor.isAnnotationPresent(Inject.class)
 * );
 *
 * // Find constructor by parameter types
 * Optional<Constructor<User>> constructor = ReflectionQuery.findConstructor(
 *     User.class,
 *     String.class, int.class
 * );
 * }</pre>
 *
 * <h2>Usage Example: Annotation Queries</h2>
 * <pre>{@code
 * // Find all annotations on a class
 * List<Annotation> classAnnotations = ReflectionQuery.getAnnotations(
 *     UserService.class
 * );
 *
 * // Find specific annotation
 * Optional<RuntimeDefinition> runtimeDef = ReflectionQuery.findAnnotation(
 *     OrderProcessing.class,
 *     RuntimeDefinition.class
 * );
 *
 * // Check if annotation is present
 * boolean isProvider = ReflectionQuery.hasAnnotation(
 *     method,
 *     Provider.class
 * );
 * }</pre>
 *
 * <h2>Usage Example: Type Queries</h2>
 * <pre>{@code
 * // Find all interfaces implemented by class
 * List<Class<?>> interfaces = ReflectionQuery.getInterfaces(UserService.class);
 *
 * // Find all superclasses
 * List<Class<?>> superclasses = ReflectionQuery.getSuperclasses(UserService.class);
 *
 * // Check if class implements interface
 * boolean isRepository = ReflectionQuery.implementsInterface(
 *     UserRepositoryImpl.class,
 *     Repository.class
 * );
 *
 * // Get generic type parameters
 * Type[] typeParams = ReflectionQuery.getGenericTypeParameters(
 *     Repository.class
 * );
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Field queries by name, type, annotation, modifier</li>
 *   <li>Method queries by name, signature, annotation, modifier</li>
 *   <li>Constructor queries by signature, annotation</li>
 *   <li>Annotation discovery and introspection</li>
 *   <li>Type hierarchy traversal</li>
 *   <li>Generic type resolution</li>
 *   <li>Predicate-based filtering</li>
 *   <li>Support for inherited members</li>
 *   <li>Performance optimization through caching</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.query;
