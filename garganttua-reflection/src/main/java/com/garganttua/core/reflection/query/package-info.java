/**
 * Reflection-based query utilities for discovering class members.
 *
 * <h2>Overview</h2>
 * <p>
 * This package resolves dotted member paths within a class hierarchy. Given an element
 * name or {@link com.garganttua.core.reflection.ObjectAddress}, an
 * {@link com.garganttua.core.reflection.IObjectQuery} locates the matching fields and
 * methods, recursing through superclasses, collection/array element types and map
 * key/value types.
 * </p>
 *
 * <h2>Usage Example: Resolving an Address</h2>
 * <pre>{@code
 * IClass<User> owner = provider.getClass(User.class);
 * IObjectQuery<User> query = ObjectQueryFactory.objectQuery(owner, provider);
 *
 * // Resolve the address of a (possibly nested) element by name
 * ObjectAddress address = query.address("email");
 *
 * // Resolve all addresses that match the element name
 * List<ObjectAddress> addresses = query.addresses("name");
 * }</pre>
 *
 * <h2>Usage Example: Resolving Member Paths</h2>
 * <pre>{@code
 * // The single best path (chain of members) for an address
 * List<Object> path = query.find("inner.value");
 *
 * // Every matching path (e.g. overloaded methods at the leaf)
 * List<List<Object>> allPaths = query.findAll("compute");
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Field and method resolution by dotted address</li>
 *   <li>Type hierarchy traversal (superclasses and interfaces)</li>
 *   <li>Recursion into collection, array and map element/key/value types</li>
 *   <li>Generic type resolution via the reflection provider</li>
 *   <li>Support for inherited members</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.query;
