package com.garganttua.core.reflection;

import java.util.List;

/**
 * Query interface for reflective class structure exploration.
 *
 * <p>
 * {@code IObjectQuery} provides an abstraction for navigating class graphs
 * using string-based addresses or {@link ObjectAddress} instances. It supports
 * finding fields and methods by name or address, resolving addresses within
 * the class hierarchy. This is fundamental for data binding, property access,
 * dynamic proxies, and reflection-based frameworks.
 * </p>
 *
 * <p>
 * For field access (get/set), use {@link com.garganttua.core.reflection.fields.FieldAccessor}
 * with {@link com.garganttua.core.reflection.fields.FieldResolver}.
 * For method invocation, use {@link com.garganttua.core.reflection.methods.MethodInvoker}
 * with {@link com.garganttua.core.reflection.methods.MethodResolver}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IObjectQuery query = ...;
 *
 * // Finding fields/methods by address
 * List<Object> path = query.find("users.profiles.name");
 *
 * // Resolving an address
 * ObjectAddress addr = query.address("email");
 *
 * // Finding all overloaded methods
 * List<List<Object>> allPaths = query.findAll("process");
 * }</pre>
 *
 * <h2>Address Formats</h2>
 * <ul>
 *   <li><b>Simple field</b>: "fieldName" - Direct field access</li>
 *   <li><b>Nested fields</b>: "field1.field2.field3" - Dot-separated navigation</li>
 *   <li><b>Method name</b>: Specified via method name at the end of the path</li>
 *   <li><b>Map access</b>: "map#key" or "map#value" - Special indicators</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see ObjectAddress
 * @see ReflectionException
 */
public interface IObjectQuery<T> {

	/**
	 * Finds objects matching the specified address.
	 *
	 * <p>
	 * This method searches for fields/methods at the given address path,
	 * returning the resolved path as a list of {@link IField} and/or {@link IMethod}
	 * instances.
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
	 * Finds all paths matching the specified address, including all overloaded methods.
	 *
	 * <p>
	 * Unlike {@link #find(ObjectAddress)} which returns only the first match,
	 * this method returns ALL matching paths. This is particularly useful for methods,
	 * where multiple overloads may share the same name.
	 * </p>
	 *
	 * @param address the object address to search
	 * @return a list of paths, each path being a list of IField/IMethod instances
	 * @throws ReflectionException if the address is invalid or search fails
	 */
	List<List<Object>> findAll(ObjectAddress address) throws ReflectionException;

	/**
	 * Finds all paths matching the specified address string, including all overloaded methods.
	 *
	 * @param address the dot-separated address string
	 * @return a list of paths, each path being a list of IField/IMethod instances
	 * @throws ReflectionException if the address is invalid or search fails
	 */
	List<List<Object>> findAll(String address) throws ReflectionException;

	/**
	 * Constructs an ObjectAddress from an element name.
	 *
	 * <p>
	 * This method returns the first matching element (field or method). For overloaded
	 * methods, use {@link #addresses(String)} to get all variants.
	 * </p>
	 *
	 * @param elementName the name of the element to create an address for
	 * @return an {@link ObjectAddress} representing the element
	 * @throws ReflectionException if the address cannot be constructed
	 */
	ObjectAddress address(String elementName) throws ReflectionException;

	/**
	 * Constructs ObjectAddress instances for all elements matching the given name.
	 *
	 * <p>
	 * Unlike {@link #address(String)} which returns only the first match, this method
	 * returns ALL matching elements. This is particularly useful for methods, where
	 * multiple overloads may share the same name. For fields, this typically returns
	 * a single-element list.
	 * </p>
	 *
	 * @param elementName the name of the element(s) to create addresses for
	 * @return a list of {@link ObjectAddress} instances representing all matching elements
	 *         (may be empty, never {@code null})
	 * @throws ReflectionException if the addresses cannot be constructed
	 */
	List<ObjectAddress> addresses(String elementName) throws ReflectionException;

}
