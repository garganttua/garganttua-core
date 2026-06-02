/**
 * Synthetic {@link java.lang.reflect.Type} implementations used to construct generic
 * type instances at runtime.
 *
 * <h2>Overview</h2>
 * <p>
 * The JDK does not expose constructors for {@link java.lang.reflect.ParameterizedType}
 * or {@link java.lang.reflect.WildcardType}. This package provides lightweight
 * implementations so the reflection layer can build generic types
 * (e.g. {@code List<String>}, {@code ? extends Number}) that no reflectable declaration
 * exists for.
 * </p>
 *
 * <h2>Usage Example: Building a Parameterized Type</h2>
 * <pre>{@code
 * // List<String>
 * ParameterizedType listOfString = new ParameterizedTypeImpl(
 *     List.class, new Type[]{ String.class });
 * }</pre>
 *
 * <h2>Usage Example: Building Wildcards</h2>
 * <pre>{@code
 * WildcardType extendsNumber = WildcardTypeImpl.extends_(Number.class); // ? extends Number
 * WildcardType superInteger  = WildcardTypeImpl.super_(Integer.class);  // ? super Integer
 * WildcardType unbounded     = WildcardTypeImpl.unbounded();            // ?
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.utils;
