/**
 * Fluent builder API implementations for constructing reflection-based binders.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of the fluent DSL interfaces defined
 * in garganttua-commons for building field, method, and constructor binders. It implements
 * the builder pattern to provide a type-safe, readable API for configuring reflection-based
 * binding scenarios.
 * </p>
 *
 * <h2>Implementation Classes</h2>
 * <p>
 * This package contains implementations of the builder interfaces from
 * {@link com.garganttua.core.reflection.binders.dsl} (commons package).
 * </p>
 *
 * <h2>Usage Example: Constructor Binder Builder (from ConstructorBinderBuilderTest)</h2>
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
 * }</pre>
 *
 * <h2>Usage Example: Constructor with Suppliers (from ConstructorBinderBuilderTest)</h2>
 * <pre>{@code
 * // Build constructor with suppliers for dynamic values
 * ConcreteConstructorBinderBuilder builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
 * builder.withParam(new FixedSupplierBuilder<>("Dynamic"))
 *        .withParam(new FixedSupplierBuilder<>(999));
 *
 * IConstructorBinder<TargetClass> binder = builder.build();
 * TargetClass tc = binder.execute().get();
 * assertEquals("Dynamic", tc.name);
 * assertEquals(999, tc.value);
 *
 * // Use default constructor (no parameters)
 * builder = new ConcreteConstructorBinderBuilder(TargetClass.class);
 * binder = builder.build();
 * tc = binder.execute().get();
 * assertEquals("default", tc.name);
 * assertEquals(-1, tc.value);
 * }</pre>
 *
 * <h2>Usage Example: Method Binder Builder (from MethodBinderTest)</h2>
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
 * // Build and invoke instance method
 * ConcreteMethodBinderBuilder b = new ConcreteMethodBinderBuilder(
 *     new Object(),
 *     FixedSupplierBuilder.of(new MethodObject())
 * );
 * b.method("echo").withReturn(String.class).withParam("Hello");
 * IMethodBinder<String> mb = b.build();
 * assertEquals("Hello", mb.supply().get());
 *
 * // Build and invoke static method
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
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe configuration</li>
 *   <li>Multiple value sources (direct, property, bean, supplier)</li>
 *   <li>Property placeholder support</li>
 *   <li>Bean injection integration</li>
 *   <li>Template string support</li>
 *   <li>Default value handling</li>
 *   <li>Null safety</li>
 *   <li>Clear error messages</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.binders.dsl
 * @see com.garganttua.core.reflection.binders
 */
package com.garganttua.core.reflection.binders.dsl;
