package com.garganttua.core.reflection;

import java.util.List;

/**
 * Query interface for reflective object navigation, field access, and method invocation.
 *
 * <p>
 * {@code IObjectQuery} provides a powerful abstraction for navigating and manipulating
 * object graphs using string-based addresses or {@link ObjectAddress} instances. It supports
 * finding objects, reading/writing field values, invoking methods, and building structured
 * representations of object fields. This is fundamental for data binding, property access,
 * dynamic proxies, and reflection-based frameworks.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IObjectQuery query = ...;
 * User user = new User();
 *
 * // Finding objects by address
 * List<Object> profiles = query.find("users.profiles");
 *
 * // Getting field values
 * Object email = query.getValue(user, "profile.contactInfo.email");
 * // Equivalent to: user.getProfile().getContactInfo().getEmail()
 *
 * // Setting field values
 * query.setValue(user, "profile.name", "Alice");
 * // Sets: user.getProfile().setName("Alice")
 *
 * // Method invocation
 * Object result = query.invoke(user, "updateProfile", profileData);
 *
 * // Building field structure
 * Object structure = query.fieldValueStructure("app.config.database");
 * }</pre>
 *
 * <h2>Address Formats</h2>
 * <ul>
 *   <li><b>Simple field</b>: "fieldName" - Direct field access</li>
 *   <li><b>Nested fields</b>: "field1.field2.field3" - Dot-separated navigation</li>
 *   <li><b>Method calls</b>: Specified via method name and arguments</li>
 *   <li><b>Map access</b>: "map#key" or "map#value" - Special indicators</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the implementation and the target objects being queried.
 * If multiple threads access or modify the same object through this query interface,
 * external synchronization may be required.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see ObjectAddress
 * @see ReflectionException
 */
public interface IObjectQuery {

	/**
	 * Finds objects matching the specified address.
	 *
	 * <p>
	 * This method searches for objects at the given address path, potentially
	 * returning multiple matches if the path includes collections or maps.
	 * </p>
	 *
	 * @param address the object address to search
	 * @return a list of objects found at the address (may be empty, never {@code null})
	 * @throws ReflectionException if the address is invalid or search fails
	 */
	List<Object> find(ObjectAddress address) throws ReflectionException;

	/**
	 * Finds objects matching the specified address string.
	 *
	 * <p>
	 * Convenience method that constructs an {@link ObjectAddress} from the string
	 * and delegates to {@link #find(ObjectAddress)}.
	 * </p>
	 *
	 * @param address the dot-separated address string
	 * @return a list of objects found at the address (may be empty, never {@code null})
	 * @throws ReflectionException if the address is invalid or search fails
	 */
	List<Object> find(String address) throws ReflectionException;

	/**
	 * Constructs an ObjectAddress from an element name.
	 *
	 * @param elementName the name of the element to create an address for
	 * @return an {@link ObjectAddress} representing the element
	 * @throws ReflectionException if the address cannot be constructed
	 */
	ObjectAddress address(String elementName) throws ReflectionException;

	/**
	 * Builds a structured representation of field values at the specified address.
	 *
	 * <p>
	 * This method creates a structure (typically a map or custom object) representing
	 * the fields and their values found at the given address. Useful for introspection
	 * and dynamic object analysis.
	 * </p>
	 *
	 * @param address the address to analyze
	 * @return a structured representation of the field values
	 * @throws ReflectionException if the address is invalid or field access fails
	 */
	Object fieldValueStructure(ObjectAddress address) throws ReflectionException;

	/**
	 * Builds a structured representation of field values at the specified address string.
	 *
	 * <p>
	 * Convenience method that constructs an {@link ObjectAddress} from the string
	 * and delegates to {@link #fieldValueStructure(ObjectAddress)}.
	 * </p>
	 *
	 * @param address the dot-separated address string
	 * @return a structured representation of the field values
	 * @throws ReflectionException if the address is invalid or field access fails
	 */
	Object fieldValueStructure(String address) throws ReflectionException;

	/**
	 * Sets a field value on the specified object using a string address.
	 *
	 * @param object the object whose field to set
	 * @param fieldAddress the dot-separated field path
	 * @param fieldValue the value to assign to the field
	 * @return the modified object
	 * @throws ReflectionException if the field cannot be accessed or set
	 */
	Object setValue(Object object, String fieldAddress, Object fieldValue) throws ReflectionException;

	/**
	 * Sets a field value on the specified object using an ObjectAddress.
	 *
	 * @param object the object whose field to set
	 * @param fieldAddress the object address representing the field path
	 * @param fieldValue the value to assign to the field
	 * @return the modified object
	 * @throws ReflectionException if the field cannot be accessed or set
	 */
	Object setValue(Object object, ObjectAddress fieldAddress, Object fieldValue) throws ReflectionException;

	/**
	 * Sets a field value using a string address (context-based object resolution).
	 *
	 * <p>
	 * This method resolves the target object from the query context and sets
	 * the field value at the specified address.
	 * </p>
	 *
	 * @param fieldAddress the dot-separated field path
	 * @param fieldValue the value to assign to the field
	 * @return the modified object
	 * @throws ReflectionException if the object or field cannot be resolved or set
	 */
	Object setValue(String fieldAddress, Object fieldValue) throws ReflectionException;

	/**
	 * Sets a field value using an ObjectAddress (context-based object resolution).
	 *
	 * <p>
	 * This method resolves the target object from the query context and sets
	 * the field value at the specified address.
	 * </p>
	 *
	 * @param fieldAddress the object address representing the field path
	 * @param fieldValue the value to assign to the field
	 * @return the modified object
	 * @throws ReflectionException if the object or field cannot be resolved or set
	 */
	Object setValue(ObjectAddress fieldAddress, Object fieldValue) throws ReflectionException;

	/**
	 * Gets a field value from the specified object using a string address.
	 *
	 * @param object the object whose field to read
	 * @param fieldAddress the dot-separated field path
	 * @return the field value, or {@code null} if the field is null
	 * @throws ReflectionException if the field cannot be accessed
	 */
	Object getValue(Object object, String fieldAddress) throws ReflectionException;

	/**
	 * Gets a field value from the specified object using an ObjectAddress.
	 *
	 * @param object the object whose field to read
	 * @param fieldAddress the object address representing the field path
	 * @return the field value, or {@code null} if the field is null
	 * @throws ReflectionException if the field cannot be accessed
	 */
	Object getValue(Object object, ObjectAddress fieldAddress) throws ReflectionException;

	/**
	 * Gets a field value using a string address (context-based object resolution).
	 *
	 * <p>
	 * This method resolves the target object from the query context and retrieves
	 * the field value at the specified address.
	 * </p>
	 *
	 * @param fieldAddress the dot-separated field path
	 * @return the field value, or {@code null} if the field is null
	 * @throws ReflectionException if the object or field cannot be resolved
	 */
	Object getValue(String fieldAddress) throws ReflectionException;

	/**
	 * Gets a field value using an ObjectAddress (context-based object resolution).
	 *
	 * <p>
	 * This method resolves the target object from the query context and retrieves
	 * the field value at the specified address.
	 * </p>
	 *
	 * @param fieldAddress the object address representing the field path
	 * @return the field value, or {@code null} if the field is null
	 * @throws ReflectionException if the object or field cannot be resolved
	 */
	Object getValue(ObjectAddress fieldAddress) throws ReflectionException;

	/**
	 * Invokes a method using a string address (context-based object resolution).
	 *
	 * <p>
	 * This method resolves the target object and method from the query context,
	 * then invokes the method with the provided arguments.
	 * </p>
	 *
	 * @param methodAddress the dot-separated method address
	 * @param args the arguments to pass to the method
	 * @return the method's return value, or {@code null} for void methods
	 * @throws ReflectionException if the method cannot be found or invoked
	 */
	Object invoke(String methodAddress, Object ...args) throws ReflectionException;

	/**
	 * Invokes a method using an ObjectAddress (context-based object resolution).
	 *
	 * <p>
	 * This method resolves the target object and method from the query context,
	 * then invokes the method with the provided arguments.
	 * </p>
	 *
	 * @param methodAddress the object address representing the method path
	 * @param args the arguments to pass to the method
	 * @return the method's return value, or {@code null} for void methods
	 * @throws ReflectionException if the method cannot be found or invoked
	 */
	Object invoke(ObjectAddress methodAddress, Object ...args) throws ReflectionException;

	/**
	 * Invokes a method on the specified object using an ObjectAddress.
	 *
	 * @param object the object on which to invoke the method
	 * @param methodAddress the object address representing the method path
	 * @param args the arguments to pass to the method
	 * @return the method's return value, or {@code null} for void methods
	 * @throws ReflectionException if the method cannot be found or invoked
	 */
	Object invoke(Object object, ObjectAddress methodAddress, Object ...args) throws ReflectionException;

}