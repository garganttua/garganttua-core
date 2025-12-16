/**
 * Advanced reflection utilities and object manipulation implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of reflection utilities for the
 * Garganttua framework. It implements high-level abstractions over Java's reflection
 * API, providing type-safe field access, method invocation, constructor binding, and
 * object introspection capabilities.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code ObjectAccessor} - Unified object access and manipulation interface</li>
 * </ul>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.fields} - Field access and manipulation</li>
 *   <li>{@link com.garganttua.core.reflection.methods} - Method invocation and binding</li>
 *   <li>{@link com.garganttua.core.reflection.constructors} - Constructor access and instantiation</li>
 *   <li>{@link com.garganttua.core.reflection.query} - Reflection-based queries</li>
 *   <li>{@link com.garganttua.core.reflection.binders} - Binder implementations</li>
 *   <li>{@link com.garganttua.core.reflection.utils} - Reflection utility classes</li>
 * </ul>
 *
 * <h2>Usage Example: ObjectAddress (from ObjectAddressTest)</h2>
 * <pre>{@code
 * // Create address with validation
 * ObjectAddress address = new ObjectAddress("field1.field2.field3");
 * assertEquals(3, address.length());
 * assertEquals("field1", address.getElement(0));
 * assertEquals("field2", address.getElement(1));
 * assertEquals("field3", address.getElement(2));
 *
 * // Convert to string
 * assertEquals("field1.field2.field3", address.toString());
 *
 * // Invalid addresses throw IllegalArgumentException
 * assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(".field1.field2"));
 * assertThrows(IllegalArgumentException.class, () -> new ObjectAddress("field1.field2."));
 * }</pre>
 *
 * <h2>Usage Example: Field Getting (from ObjectFieldGetterTest)</h2>
 * <pre>{@code
 * class ObjectTest {
 *     private long l;
 *     private ObjectTest inner;
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
 * nestedInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "l"));
 *
 * ObjectAddress nestedAddress = new ObjectAddress("inner.l");
 * ObjectFieldGetter nestedGetter = new ObjectFieldGetter(ObjectTest.class, nestedInfos, nestedAddress);
 *
 * Object nestedValue = nestedGetter.getValue(o);
 * assertEquals(1L, nestedValue);
 * }</pre>
 *
 * <h2>Usage Example: Field Setting (from ObjectFieldSetterTest)</h2>
 * <pre>{@code
 * class ObjectTest {
 *     private String s;
 *     private List<ObjectTest> innersInList;
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
 * // Set values in list - creates list elements automatically
 * List<Object> listInfos = new ArrayList<Object>();
 * listInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
 * listInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "s"));
 *
 * ObjectAddress listAddress = new ObjectAddress("innersInList.s");
 * ObjectFieldSetter listSetter = new ObjectFieldSetter(ObjectTest.class, listInfos, listAddress);
 *
 * ObjectTest listObject = (ObjectTest) listSetter.setValue(List.of("a", "b", "c"));
 * assertEquals(3, listObject.getInnersInList().size());
 * }</pre>
 *
 * <h2>Usage Example: Constructor Binder (from ConstructorBinderBuilderTest)</h2>
 * <pre>{@code
 * class TargetClass {
 *     public final String name;
 *     public final int value;
 *
 *     public TargetClass(String name, int value) {
 *         this.name = name;
 *         this.value = value;
 *     }
 * }
 *
 * // Build constructor binder with parameters
 * ConcreteConstructorBinderBuilder builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
 * builder.withParam("Hello").withParam(123);
 *
 * IConstructorBinder<TargetClass> binder = builder.build();
 * Optional<? extends TargetClass> obj = binder.execute();
 * TargetClass tc = obj.get();
 * assertEquals("Hello", tc.name);
 * assertEquals(123, tc.value);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe field access (get/set)</li>
 *   <li>Method invocation with parameter binding</li>
 *   <li>Constructor invocation with parameter binding</li>
 *   <li>Annotation-based queries</li>
 *   <li>Access modifier handling (private, protected, public)</li>
 *   <li>Generic type preservation</li>
 *   <li>Automatic type conversion</li>
 *   <li>Null safety</li>
 *   <li>Exception wrapping</li>
 *   <li>Performance optimization through caching</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * This reflection implementation is used by:
 * </p>
 * <ul>
 *   <li>Dependency injection framework for bean instantiation and injection</li>
 *   <li>Runtime execution framework for method invocation</li>
 *   <li>Object mapper for field-to-field copying</li>
 *   <li>Property binding for configuration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.fields
 * @see com.garganttua.core.reflection.methods
 * @see com.garganttua.core.reflection.constructors
 * @see com.garganttua.core.reflection.binders
 */
package com.garganttua.core.reflection;
