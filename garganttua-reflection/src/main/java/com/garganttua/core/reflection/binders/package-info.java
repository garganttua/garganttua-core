/**
 * Reflection-based binding implementations for fields, methods, and constructors.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of binder interfaces defined in
 * garganttua-commons. It enables type-safe, declarative configuration of object
 * construction and initialization through reflection.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code FieldBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IFieldBinder}</li>
 *   <li>{@code MethodBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IMethodBinder}</li>
 *   <li>{@code ConstructorBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IConstructorBinder}</li>
 *   <li>{@code ExecutableBinder} - Implementation of {@link com.garganttua.core.reflection.binders.IExecutableBinder}</li>
 * </ul>
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
 *
 *     public TargetClass(String name) {
 *         this(name, 0);
 *     }
 *
 *     public TargetClass() {
 *         this("default", -1);
 *     }
 * }
 *
 * // Build constructor binder with raw values
 * ConcreteConstructorBinderBuilder builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
 * builder.withParam("Hello").withParam(123);
 *
 * IConstructorBinder<TargetClass> binder = builder.build();
 * Optional<? extends TargetClass> obj = binder.execute();
 * TargetClass tc = obj.get();
 * assertEquals("Hello", tc.name);
 * assertEquals(123, tc.value);
 *
 * // Build constructor with suppliers
 * builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
 * builder.withParam(new FixedSupplierBuilder<>("Dynamic"))
 *        .withParam(new FixedSupplierBuilder<>(999));
 *
 * binder = builder.build();
 * tc = binder.execute().get();
 * assertEquals("Dynamic", tc.name);
 * assertEquals(999, tc.value);
 *
 * // Nullable parameters
 * builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
 * builder.withParam(0, new NullSupplierBuilder<String>(String.class), true);
 * builder.withParam(1, 77);
 *
 * binder = builder.build();
 * tc = binder.execute().get();
 * assertNull(tc.name);
 * assertEquals(77, tc.value);
 * }</pre>
 *
 * <h2>Usage Example: Method Binder (from MethodBinderTest)</h2>
 * <pre>{@code
 * class MethodObject {
 *     String echo(String message) {
 *         return message;
 *     }
 *
 *     static String staticEcho(String message) {
 *         return message;
 *     }
 * }
 *
 * // Invoke instance method
 * ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(
 *     new Object(),
 *     FixedSupplierBuilder.of(new MethodObject())
 * );
 * b.method("echo").withReturn(String.class).withParam("Hello");
 * IMethodBinder<String> mb = b.build();
 * assertEquals("Hello", mb.supply().get());
 *
 * // Invoke static method (supplier returns null for static)
 * ConcreteMethodBinderBuilder staticBuilder = new ConcreteMethodBinderBuilder(
 *     new Object(),
 *     new NullSupplierBuilder<>(MethodObject.class)
 * );
 * staticBuilder.method("staticEcho").withReturn(String.class).withParam("Hello");
 * IMethodBinder<String> staticBinder = staticBuilder.build();
 * assertEquals("Hello", staticBinder.supply().get());
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe parameter binding</li>
 *   <li>Support for primitive and object types</li>
 *   <li>Automatic type conversion</li>
 *   <li>Accessibility management</li>
 *   <li>Null safety</li>
 *   <li>Exception handling and wrapping</li>
 *   <li>Generic type preservation</li>
 *   <li>Performance optimization</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.reflection.binders.dsl} - Builder implementations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.binders
 * @see com.garganttua.core.reflection
 */
package com.garganttua.core.reflection.binders;
