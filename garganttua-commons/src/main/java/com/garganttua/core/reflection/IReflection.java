package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * Unified facade for the Garganttua reflection subsystem.
 *
 * <p>
 * {@code IReflection} provides a single entry point that unifies all the
 * utility
 * classes of the {@code garganttua-reflection} module:
 * </p>
 * <ul>
 * <li>{@code ObjectReflectionHelper} — constructor/field/method lookup,
 * instantiation,
 * field access, method invocation, type utilities</li>
 * <li>{@code ObjectQueryFactory} / {@code ObjectQuery} — object graph
 * navigation</li>
 * <li>{@code MethodResolver} — method resolution by name, signature, or
 * address</li>
 * <li>{@code FieldResolver} — field resolution by name, address, or
 * annotation</li>
 * <li>{@code MethodInvoker} — deep method invocation through object paths</li>
 * <li>{@code ObjectAccessor} — high-level functional get/set/invoke</li>
 * <li>{@code Fields} — field type classification and generic type
 * extraction</li>
 * <li>{@code Methods} — method property checks</li>
 * </ul>
 *
 * <p>
 * It extends {@link IReflectionProvider} which provides the core class
 * resolution
 * methods ({@code getClass}, {@code forName}). All returned members use the
 * {@code I*} abstraction interfaces ({@link IField}, {@link IMethod},
 * {@link IConstructor}, etc.) so that implementations can be backed by either
 * runtime reflection or AOT-generated descriptors.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IReflectionProvider
 */
public interface IReflection extends IReflectionProvider, IAnnotationScanner {

	// ========================================================================
	// Object Query Factory (facade for ObjectQueryFactory / ObjectQuery)
	// ========================================================================

	/**
	 * Creates an object query for navigating the structure of the given class.
	 *
	 * @param <T>         the object type
	 * @param objectClass the class to introspect
	 * @return a new object query instance
	 */
	<T> IObjectQuery<T> query(IClass<T> objectClass) throws ReflectionException;

	/**
	 * Creates an object query bound to a specific object instance.
	 *
	 * @param <T>    the object type
	 * @param object the object to bind the query to
	 * @return a new object query instance
	 */
	<T> IObjectQuery<T> query(T object) throws ReflectionException;

	/**
	 * Creates an object query for the given class, bound to a specific instance.
	 *
	 * @param <T>         the object type
	 * @param objectClass the class to introspect
	 * @param object      the object to bind the query to
	 * @return a new object query instance
	 */
	<T> IObjectQuery<T> query(IClass<T> objectClass, T object) throws ReflectionException;

	// ========================================================================
	// Constructor Lookup (facade for ObjectReflectionHelper constructors)
	// ========================================================================

	/**
	 * Finds the no-arg constructor for the given class.
	 *
	 * @param clazz the class to search
	 * @return the no-arg constructor, or empty if not found
	 */
	Optional<IConstructor<?>> findConstructor(IClass<?> clazz);

	/**
	 * Finds a constructor matching the given parameter types.
	 * Handles primitive/wrapper assignability.
	 *
	 * @param clazz          the class to search
	 * @param parameterTypes the expected constructor parameter types
	 * @return the matching constructor, or empty if not found
	 */
	Optional<IConstructor<?>> findConstructor(IClass<?> clazz, IClass<?>... parameterTypes);

	// ========================================================================
	// Instantiation (facade for ObjectReflectionHelper.instanciateNewObject)
	// ========================================================================

	/**
	 * Instantiates a new object using the no-arg constructor.
	 *
	 * @param <T>   the type to instantiate
	 * @param clazz the class to instantiate
	 * @return the new instance
	 * @throws ReflectionException if no suitable constructor is found or
	 *                             instantiation fails
	 */
	<T> T newInstance(IClass<T> clazz) throws ReflectionException;

	/**
	 * Instantiates a new object using a constructor matching the given arguments.
	 *
	 * @param <T>   the type to instantiate
	 * @param clazz the class to instantiate
	 * @param args  the constructor arguments
	 * @return the new instance
	 * @throws ReflectionException if no suitable constructor is found or
	 *                             instantiation fails
	 */
	<T> T newInstance(IClass<T> clazz, Object... args) throws ReflectionException;

	/**
	 * Instantiates a new object using the no-arg constructor, forcing access
	 * even if the constructor is private or protected.
	 */
	<T> T newInstance(IClass<T> clazz, boolean force) throws ReflectionException;

	/**
	 * Instantiates a new object using a constructor matching the given arguments,
	 * forcing access even if the constructor is private or protected.
	 */
	<T> T newInstance(IClass<T> clazz, boolean force, Object... args) throws ReflectionException;

	// ========================================================================
	// Field Lookup (facade for ObjectReflectionHelper field methods)
	// ========================================================================

	/**
	 * Finds a field by name, searching the class hierarchy recursively.
	 *
	 * @param clazz     the class to search
	 * @param fieldName the name of the field
	 * @return the field, or empty if not found
	 */
	Optional<IField> findField(IClass<?> clazz, String fieldName);

	/**
	 * Finds the first field annotated with the given annotation.
	 * Searches the class hierarchy and nested objects recursively.
	 *
	 * @param clazz      the class to search
	 * @param annotation the annotation to look for
	 * @return the annotated field, or empty if not found
	 */
	Optional<IField> findFieldAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation);

	/**
	 * Finds all field address paths annotated with the given annotation.
	 *
	 * @param clazz      the class to search
	 * @param annotation the annotation to look for
	 * @param linked     if true, only recurse into sub-objects when the annotation
	 *                   is found
	 * @return list of dot-separated field address strings
	 */
	List<String> findFieldAddressesWithAnnotation(IClass<?> clazz, IClass<? extends Annotation> annotation,
			boolean linked);

	// ========================================================================
	// Field Access (facade for FieldAccessor / ResolvedField)
	// ========================================================================

	/**
	 * Gets a field value by name from an object.
	 *
	 * @param object    the object to read from
	 * @param fieldName the name of the field
	 * @return the field value
	 * @throws ReflectionException if the field cannot be found or accessed
	 */
	Object getFieldValue(Object object, String fieldName) throws ReflectionException;

	/**
	 * Gets a field value using an {@link IField} descriptor.
	 *
	 * @param object the object to read from
	 * @param field  the field descriptor
	 * @return the field value
	 * @throws ReflectionException if the field cannot be accessed
	 */
	Object getFieldValue(Object object, IField field) throws ReflectionException;

	/**
	 * Sets a field value by name on an object.
	 *
	 * @param object    the object to modify
	 * @param fieldName the name of the field
	 * @param value     the value to set
	 * @throws ReflectionException if the field cannot be found or set
	 */
	void setFieldValue(Object object, String fieldName, Object value) throws ReflectionException;

	/**
	 * Sets a field value using an {@link IField} descriptor.
	 *
	 * @param object the object to modify
	 * @param field  the field descriptor
	 * @param value  the value to set
	 * @throws ReflectionException if the field cannot be set
	 */
	void setFieldValue(Object object, IField field, Object value) throws ReflectionException;

	/**
	 * Gets a field value by name, forcing access even if the field is private or protected.
	 */
	Object getFieldValue(Object object, String fieldName, boolean force) throws ReflectionException;

	/**
	 * Gets a field value using an {@link IField} descriptor, forcing access
	 * even if the field is private or protected.
	 */
	Object getFieldValue(Object object, IField field, boolean force) throws ReflectionException;

	/**
	 * Sets a field value by name, forcing access even if the field is private or protected.
	 */
	void setFieldValue(Object object, String fieldName, Object value, boolean force) throws ReflectionException;

	/**
	 * Sets a field value using an {@link IField} descriptor, forcing access
	 * even if the field is private or protected.
	 */
	void setFieldValue(Object object, IField field, Object value, boolean force) throws ReflectionException;

	// ========================================================================
	// Field Resolution (facade for FieldResolver)
	// ========================================================================

	/**
	 * Resolves a field address by name within the given class.
	 *
	 * @param fieldName   the field name to resolve
	 * @param entityClass the class to search
	 * @return the resolved address, or empty if not found
	 */
	Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass);

	/**
	 * Resolves a field address by name with type validation.
	 *
	 * @param fieldName   the field name to resolve
	 * @param entityClass the class to search
	 * @param fieldType   the expected field type
	 * @return the resolved address, or empty if not found or type mismatch
	 */
	Optional<ObjectAddress> resolveFieldAddress(String fieldName, IClass<?> entityClass, IClass<?> fieldType);

	/**
	 * Validates an existing field address against the given class.
	 *
	 * @param address     the address to validate
	 * @param entityClass the class to validate against
	 * @return the validated address, or empty if invalid
	 */
	Optional<ObjectAddress> resolveFieldAddress(ObjectAddress address, IClass<?> entityClass);

	// ========================================================================
	// Method Lookup (facade for ObjectReflectionHelper method methods)
	// ========================================================================

	/**
	 * Finds the first method by name, searching the class hierarchy recursively.
	 *
	 * @param clazz      the class to search
	 * @param methodName the name of the method
	 * @return the method, or empty if not found
	 */
	Optional<IMethod> findMethod(IClass<?> clazz, String methodName);

	/**
	 * Finds all methods with the given name (including overloads),
	 * searching the class hierarchy recursively. Deduplicates inherited methods.
	 *
	 * @param clazz      the class to search
	 * @param methodName the name of the methods
	 * @return list of matching methods (may be empty, never null)
	 */
	List<IMethod> findMethods(IClass<?> clazz, String methodName);

	/**
	 * Finds the first method annotated with the given annotation,
	 * searching the class hierarchy recursively.
	 *
	 * @param clazz      the class to search
	 * @param annotation the annotation to look for
	 * @return the annotated method, or empty if not found
	 */
	Optional<IMethod> findMethodAnnotatedWith(IClass<?> clazz, IClass<? extends Annotation> annotation);

	// ========================================================================
	// Method Resolution (facade for MethodResolver)
	// ========================================================================

	/**
	 * Resolves a method by name only (must be unambiguous).
	 *
	 * @param ownerType  the class owning the method
	 * @param methodName the method name
	 * @return the resolved method, or empty if not found or ambiguous
	 */
	Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName);

	/**
	 * Resolves a method by name with full signature matching.
	 *
	 * @param ownerType      the class owning the method
	 * @param methodName     the method name
	 * @param returnType     the expected return type
	 * @param parameterTypes the expected parameter types
	 * @return the resolved method, or empty if no match
	 */
	Optional<IMethod> resolveMethod(IClass<?> ownerType, String methodName, IClass<?> returnType,
			IClass<?>... parameterTypes);

	/**
	 * Resolves a method by {@link ObjectAddress}.
	 *
	 * @param ownerType     the root class
	 * @param methodAddress the address path to the method
	 * @return the resolved method, or empty if not found
	 */
	Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress);

	/**
	 * Resolves a method by {@link ObjectAddress} with full signature matching.
	 *
	 * @param ownerType      the root class
	 * @param methodAddress  the address path to the method
	 * @param returnType     the expected return type
	 * @param parameterTypes the expected parameter types
	 * @return the resolved method, or empty if no match
	 */
	Optional<IMethod> resolveMethod(IClass<?> ownerType, ObjectAddress methodAddress, IClass<?> returnType,
			IClass<?>... parameterTypes);

	// ========================================================================
	// Method Invocation (facade for MethodInvoker + ObjectReflectionHelper)
	// ========================================================================

	/**
	 * Invokes a method on an object with type-checked return value.
	 *
	 * @param <R>        the return type
	 * @param object     the target object
	 * @param method     the method to invoke
	 * @param returnType the expected return type
	 * @param args       the method arguments
	 * @return the return value
	 * @throws ReflectionException if invocation fails or types mismatch
	 */
	<R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, Object... args) throws ReflectionException;

	/**
	 * Finds and invokes a method by name with type-checked return value.
	 *
	 * @param <R>        the return type
	 * @param object     the target object
	 * @param methodName the name of the method to invoke
	 * @param returnType the expected return type
	 * @param args       the method arguments
	 * @return the return value
	 * @throws ReflectionException if the method is not found, invocation fails, or
	 *                             types mismatch
	 */
	<R> R invokeMethod(Object object, String methodName, IClass<R> returnType, Object... args)
			throws ReflectionException;

	/**
	 * Invokes a method through a deep object path, handling nested fields,
	 * collections, maps, and arrays transparently.
	 *
	 * @param <R>        the return type
	 * @param object     the root object
	 * @param address    the dot-separated path to the method
	 * @param returnType the expected return type
	 * @param args       the method arguments
	 * @return the method return value(s)
	 * @throws ReflectionException if resolution or invocation fails
	 */
	<R> IMethodReturn<R> invokeDeep(Object object, ObjectAddress address, IClass<R> returnType, Object... args)
			throws ReflectionException;

	/**
	 * Invokes a method on an object, forcing access even if the method is
	 * private or protected.
	 */
	<R> R invokeMethod(Object object, IMethod method, IClass<R> returnType, boolean force, Object... args)
			throws ReflectionException;

	/**
	 * Finds and invokes a method by name, forcing access even if the method is
	 * private or protected.
	 */
	<R> R invokeMethod(Object object, String methodName, IClass<R> returnType, boolean force, Object... args)
			throws ReflectionException;

	/**
	 * Invokes a method through a deep object path, forcing access even if
	 * members are private or protected.
	 */
	<R> IMethodReturn<R> invokeDeep(Object object, ObjectAddress address, IClass<R> returnType, boolean force,
			Object... args) throws ReflectionException;

	// ========================================================================
	// Type Utilities (facade for ObjectReflectionHelper type methods + Fields)
	// ========================================================================

	/**
	 * Extracts the raw {@link Class} from any {@link Type}
	 * (ParameterizedType, GenericArrayType, TypeVariable, WildcardType).
	 *
	 * @param type the type to extract from
	 * @return the raw class
	 * @throws IllegalArgumentException if the type cannot be converted
	 */
	IClass<?> extractClass(Type type);

	/**
	 * Deep type equality comparison supporting ParameterizedType,
	 * GenericArrayType, and Class-level assignability.
	 *
	 * @param type1 the first type
	 * @param type2 the second type
	 * @return true if the types are considered equal
	 */
	boolean typeEquals(Type type1, Type type2);

	/**
	 * Checks whether a class directly implements a specific interface.
	 *
	 * @param interfaceType the interface to check for
	 * @param objectType    the class to test
	 * @return true if objectType implements interfaceType
	 */
	boolean isImplementingInterface(IClass<?> interfaceType, IClass<?> objectType);

	/**
	 * Extracts parameter types from an argument array.
	 * Null arguments are mapped to {@code Object.class}.
	 *
	 * @param args the argument array (may be null)
	 * @return the corresponding parameter type array
	 */
	IClass<?>[] parameterTypes(Object[] args);

	/**
	 * Checks whether a class is a non-primitive, non-wrapper, non-JDK-internal
	 * type.
	 * Useful for determining whether to recurse into an object's fields.
	 *
	 * @param clazz the class to check
	 * @return true if the class is a "complex" user type
	 */
	boolean isComplexType(IClass<?> clazz);

	/**
	 * Gets the generic type argument at the given index from a class or field type.
	 *
	 * @param type  the class whose type parameters to inspect
	 * @param index the index of the type argument (0-based)
	 * @return the generic type argument class, or null if not available
	 */
	IClass<?> getGenericTypeArgument(IClass<?> type, int index);

	/**
	 * Checks whether a field's type is a Collection, Map, or array.
	 *
	 * @param field the field to check
	 * @return true if the field type is a collection-like type
	 */
	boolean isCollectionOrMapOrArray(IField field);
}
