/**
 * Field access, manipulation, and introspection utilities.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides utilities for accessing and manipulating object fields via
 * reflection. It offers type-safe field access with automatic accessibility handling,
 * type conversion, and null safety.
 * </p>
 *
 * <h2>Usage Example: Getting Field Values</h2>
 * <pre>{@code
 * class ObjectTest {
 *     private long l;
 *     private ObjectTest inner;
 *     private float f;
 * }
 *
 * IClass<ObjectTest> owner = provider.getClass(ObjectTest.class);
 *
 * // Get a simple field value
 * ResolvedField l = FieldResolver.fieldByFieldName(owner, provider, "l");
 * Object value = new FieldAccessor<>(l).getValue(o).single();
 *
 * // Get a nested field value via a dotted address
 * ResolvedField innerF = FieldResolver.fieldByAddress(owner, provider, new ObjectAddress("inner.f"));
 * Object nested = new FieldAccessor<>(innerF).getValue(o).single();
 * }</pre>
 *
 * <h2>Usage Example: Setting Field Values</h2>
 * <pre>{@code
 * class ObjectTest {
 *     private String s;
 *     private ObjectTest inner;
 *     private long l;
 * }
 *
 * IClass<ObjectTest> owner = provider.getClass(ObjectTest.class);
 *
 * // Set a simple field
 * ResolvedField s = FieldResolver.fieldByFieldName(owner, provider, "s");
 * new FieldAccessor<String>(s).setValue(object, SingleFieldValue.of("test", provider.getClass(String.class)));
 *
 * // Set a value in a nested object via a dotted address
 * ResolvedField innerL = FieldResolver.fieldByAddress(owner, provider, new ObjectAddress("inner.l"));
 * new FieldAccessor<Long>(innerL).setValue(object, SingleFieldValue.of(1L, provider.getClass(Long.class)));
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe field access</li>
 *   <li>Automatic accessibility handling (private, protected, public)</li>
 *   <li>Type conversion support</li>
 *   <li>Null safety</li>
 *   <li>Field queries by name, type, annotation</li>
 *   <li>Field value copying</li>
 *   <li>Generic type preservation</li>
 *   <li>Exception wrapping</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.fields;
