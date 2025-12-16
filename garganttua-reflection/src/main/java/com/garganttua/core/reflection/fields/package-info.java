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
 * <h2>Usage Example: Getting Field Values (from ObjectFieldGetterTest)</h2>
 * <pre>{@code
 * class ObjectTest {
 *     private long l;
 *     private ObjectTest inner;
 *     private float f;
 * }
 *
 * // Get simple field value
 * List<Object> fieldInfos = new ArrayList<Object>();
 * fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "l"));
 *
 * ObjectAddress address = new ObjectAddress("l");
 * ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
 *
 * Object value = getter.getValue(o);
 * assertEquals(1L, value);
 *
 * // Get nested field value
 * List<Object> nestedInfos = new ArrayList<Object>();
 * nestedInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
 * nestedInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "f"));
 *
 * ObjectAddress nestedAddress = new ObjectAddress("inner.f");
 * ObjectFieldGetter nestedGetter = new ObjectFieldGetter(ObjectTest.class, nestedInfos, nestedAddress);
 *
 * Object nestedValue = nestedGetter.getValue(o);
 * assertEquals(1F, nestedValue);
 * }</pre>
 *
 * <h2>Usage Example: Setting Field Values (from ObjectFieldSetterTest)</h2>
 * <pre>{@code
 * class ObjectTest {
 *     private long l;
 *     private String s;
 *     private ObjectTest inner;
 * }
 *
 * // Set simple field
 * List<Object> fieldInfos = new ArrayList<Object>();
 * fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "s"));
 *
 * ObjectAddress address = new ObjectAddress("s");
 * ObjectFieldSetter setter = new ObjectFieldSetter(ObjectTest.class, fieldInfos, address);
 *
 * ObjectTest object = (ObjectTest) setter.setValue("test");
 * assertEquals("test", object.getS());
 *
 * // Set value in nested object
 * List<Object> innerInfos = new ArrayList<Object>();
 * innerInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
 * innerInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "l"));
 *
 * ObjectAddress innerAddress = new ObjectAddress("inner.l");
 * ObjectFieldSetter innerSetter = new ObjectFieldSetter(ObjectTest.class, innerInfos, innerAddress);
 *
 * ObjectTest innerObject = (ObjectTest) innerSetter.setValue(1L);
 * assertEquals(1L, innerObject.getInner().getL());
 * }</pre>
 *
 * <h2>Usage Example: Working with Lists (from ObjectFieldSetterTest)</h2>
 * <pre>{@code
 * class ObjectTest {
 *     private List<ObjectTest> innersInList;
 *     private long l;
 * }
 *
 * // Set values in list - creates list elements automatically
 * List<Object> fieldInfos = new ArrayList<Object>();
 * fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
 * fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "l"));
 *
 * ObjectAddress address = new ObjectAddress("innersInList.l");
 * ObjectFieldSetter setter = new ObjectFieldSetter(ObjectTest.class, fieldInfos, address);
 *
 * ObjectTest object = (ObjectTest) setter.setValue(List.of(1L, 2L, 3L));
 * assertEquals(3, object.getInnersInList().size());
 * assertEquals(1L, object.getInnersInList().get(0).getL());
 * assertEquals(2L, object.getInnersInList().get(1).getL());
 * assertEquals(3L, object.getInnersInList().get(2).getL());
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
