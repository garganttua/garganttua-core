package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IRecordComponent;

/**
 * Behavioural coverage for {@link RuntimeClass} — verifies the IClass mirror
 * delegates to the wrapped {@code Class<?>} with the expected concrete values,
 * caches by identity, and propagates the JDK exception paths.
 */
class RuntimeClassBehaviourTest {

	// --- Fixtures ---

	@Retention(RetentionPolicy.RUNTIME)
	@interface Marker {
		String value() default "x";
	}

	@Marker("on-fixture")
	static class Fixture extends Parent implements Serializable {
		public int publicField;
		private String privateField;
	}

	static class Parent {
	}

	enum Color {
		RED, GREEN, BLUE
	}

	record Point(int x, int y) {
	}

	sealed interface Shape permits Circle, Square {
	}

	record Circle(double r) implements Shape {
	}

	record Square(double s) implements Shape {
	}

	// --- Caching / identity ---

	@Test
	void of_returns_same_cached_instance_for_same_class() {
		RuntimeClass<String> a = RuntimeClass.of(String.class);
		RuntimeClass<String> b = RuntimeClass.of(String.class);
		assertSame(a, b);
	}

	@Test
	void ofUnchecked_shares_cache_with_of() {
		assertSame(RuntimeClass.of(Integer.class), RuntimeClass.ofUnchecked(Integer.class));
	}

	@Test
	void unwrap_returns_the_wrapped_class() {
		assertSame(Fixture.class, RuntimeClass.of(Fixture.class).unwrap());
	}

	// --- Naming ---

	@Test
	void naming_methods_match_jdk() {
		RuntimeClass<String> c = RuntimeClass.of(String.class);
		assertEquals("java.lang.String", c.getName());
		assertEquals("String", c.getSimpleName());
		assertEquals("java.lang.String", c.getCanonicalName());
		assertEquals("java.lang.String", c.getTypeName());
		assertEquals("java.lang", c.getPackageName());
	}

	@Test
	void descriptorString_matches_jdk() {
		assertEquals("Ljava/lang/String;", RuntimeClass.of(String.class).descriptorString());
		assertEquals("I", RuntimeClass.ofUnchecked(int.class).descriptorString());
	}

	// --- Properties ---

	@Test
	void boolean_property_methods_reflect_kind() {
		assertTrue(RuntimeClass.of(Runnable.class).isInterface());
		assertTrue(RuntimeClass.ofUnchecked(int.class).isPrimitive());
		assertTrue(RuntimeClass.ofUnchecked(int[].class).isArray());
		assertTrue(RuntimeClass.of(Color.class).isEnum());
		assertTrue(RuntimeClass.of(Point.class).isRecord());
		assertTrue(RuntimeClass.of(Marker.class).isAnnotation());
		assertTrue(RuntimeClass.of(Shape.class).isSealed());
		assertTrue(RuntimeClass.of(Fixture.class).isMemberClass());

		assertFalse(RuntimeClass.of(String.class).isInterface());
		assertFalse(RuntimeClass.of(String.class).isEnum());
		assertFalse(RuntimeClass.of(String.class).isArray());
	}

	@Test
	void getModifiers_matches_jdk() {
		assertEquals(Fixture.class.getModifiers(), RuntimeClass.of(Fixture.class).getModifiers());
	}

	// --- Type hierarchy ---

	@Test
	void getSuperclass_wraps_parent_and_returns_null_for_object() {
		IClass<?> sup = RuntimeClass.of(Fixture.class).getSuperclass();
		assertEquals(Parent.class.getName(), sup.getName());
		assertNull(RuntimeClass.of(Object.class).getSuperclass());
	}

	@Test
	void getInterfaces_lists_declared_interfaces() {
		IClass<?>[] ifaces = RuntimeClass.of(Fixture.class).getInterfaces();
		assertEquals(1, ifaces.length);
		assertEquals(Serializable.class.getName(), ifaces[0].getName());
	}

	@Test
	void getTypeParameters_returns_declared_type_variables() {
		assertEquals(1, RuntimeClass.of(List.class).getTypeParameters().length);
		assertEquals("E", RuntimeClass.of(List.class).getTypeParameters()[0].getName());
		assertEquals(0, RuntimeClass.of(String.class).getTypeParameters().length);
	}

	// --- Type checks ---

	@Test
	void isAssignableFrom_iclass_overload() {
		assertTrue(RuntimeClass.of(Parent.class).isAssignableFrom(RuntimeClass.of(Fixture.class)));
		assertFalse(RuntimeClass.of(Fixture.class).isAssignableFrom(RuntimeClass.of(Parent.class)));
	}

	@Test
	void isAssignableFrom_raw_class_overload() {
		assertTrue(RuntimeClass.of(Number.class).isAssignableFrom(Integer.class));
		assertFalse(RuntimeClass.of(Integer.class).isAssignableFrom(Number.class));
	}

	@Test
	void isInstance_checks_runtime_type() {
		assertTrue(RuntimeClass.of(CharSequence.class).isInstance("hello"));
		assertFalse(RuntimeClass.of(CharSequence.class).isInstance(42));
	}

	// --- Array ---

	@Test
	void componentType_and_arrayType_roundtrip() {
		IClass<?> arr = RuntimeClass.of(String.class).arrayType();
		assertTrue(arr.isArray());
		assertEquals(String.class.getName(), arr.getComponentType().getName());
		assertNull(RuntimeClass.of(String.class).getComponentType());
	}

	// --- Members ---

	@Test
	void getDeclaredField_returns_field_and_throws_for_missing() throws Exception {
		IField f = RuntimeClass.of(Fixture.class).getDeclaredField("publicField");
		assertEquals("publicField", f.getName());
		assertThrows(NoSuchFieldException.class,
				() -> RuntimeClass.of(Fixture.class).getDeclaredField("nope"));
	}

	@Test
	void getDeclaredMethod_with_param_types_resolves_overload() throws Exception {
		IMethod m = RuntimeClass.of(String.class)
				.getMethod("substring", RuntimeClass.ofUnchecked(int.class));
		assertEquals("substring", m.getName());
		assertEquals(1, m.getParameterCount());
		assertThrows(NoSuchMethodException.class,
				() -> RuntimeClass.of(String.class).getMethod("substring", RuntimeClass.of(String.class)));
	}

	@Test
	void getDeclaredConstructor_resolves_and_throws() throws Exception {
		IConstructor<String> ctor = RuntimeClass.of(String.class)
				.getConstructor(RuntimeClass.of(String.class));
		assertEquals(1, ctor.getParameterCount());
		assertThrows(NoSuchMethodException.class,
				() -> RuntimeClass.of(String.class).getConstructor(RuntimeClass.of(Thread.class)));
	}

	@Test
	void getDeclaredFields_includes_private_and_public() {
		IField[] fields = RuntimeClass.of(Fixture.class).getDeclaredFields();
		long count = java.util.Arrays.stream(fields)
				.filter(f -> f.getName().equals("publicField") || f.getName().equals("privateField"))
				.count();
		assertEquals(2, count);
	}

	// --- Record components ---

	@Test
	void getRecordComponents_for_record_and_null_for_nonrecord() {
		IRecordComponent[] comps = RuntimeClass.of(Point.class).getRecordComponents();
		assertEquals(2, comps.length);
		assertEquals("x", comps[0].getName());
		assertEquals("y", comps[1].getName());
		assertNull(RuntimeClass.of(String.class).getRecordComponents());
	}

	// --- Sealed ---

	@Test
	void getPermittedSubclasses_for_sealed_and_null_otherwise() {
		IClass<?>[] subs = RuntimeClass.of(Shape.class).getPermittedSubclasses();
		assertEquals(2, subs.length);
		assertNull(RuntimeClass.of(String.class).getPermittedSubclasses());
	}

	// --- Enum ---

	@Test
	void getEnumConstants_returns_values_and_null_for_nonenum() {
		Color[] vals = RuntimeClass.of(Color.class).getEnumConstants();
		assertArrayEquals(new Color[] { Color.RED, Color.GREEN, Color.BLUE }, vals);
		assertNull(RuntimeClass.of(String.class).getEnumConstants());
	}

	// --- Nesting ---

	@Test
	void getEnclosingClass_and_declaringClass() {
		assertEquals(RuntimeClassBehaviourTest.class.getName(),
				RuntimeClass.of(Fixture.class).getEnclosingClass().getName());
		assertNull(RuntimeClass.of(String.class).getEnclosingClass());
		assertEquals(RuntimeClassBehaviourTest.class.getName(),
				RuntimeClass.of(Fixture.class).getDeclaringClass().getName());
	}

	// --- Annotations ---

	@Test
	void getAnnotation_returns_present_annotation_with_value() {
		Marker m = RuntimeClass.of(Fixture.class).getAnnotation(RuntimeClass.of(Marker.class));
		assertEquals("on-fixture", m.value());
		assertTrue(RuntimeClass.of(Fixture.class).isAnnotationPresent(RuntimeClass.of(Marker.class)));
	}

	@Test
	void getAnnotation_returns_null_when_absent() {
		assertNull(RuntimeClass.of(String.class).getAnnotation(RuntimeClass.of(Marker.class)));
		assertFalse(RuntimeClass.of(String.class).isAnnotationPresent(RuntimeClass.of(Marker.class)));
	}

	// --- Cast ---

	@Test
	void cast_succeeds_and_fails_appropriately() {
		assertEquals("hi", RuntimeClass.of(String.class).cast("hi"));
		assertThrows(ClassCastException.class, () -> RuntimeClass.of(String.class).cast(42));
	}

	@Test
	void asSubclass_narrows_or_throws() {
		IClass<? extends Number> sub = RuntimeClass.of(Integer.class).asSubclass(RuntimeClass.of(Number.class));
		assertEquals(Integer.class.getName(), sub.getName());
		assertThrows(ClassCastException.class,
				() -> RuntimeClass.of(String.class).asSubclass(RuntimeClass.of(Number.class)));
	}

	// --- Runtime info ---

	@Test
	void getType_returns_the_raw_class() {
		assertSame(String.class, RuntimeClass.of(String.class).getType());
	}

	@Test
	void getClassLoader_null_for_bootstrap_and_present_for_fixture() {
		assertNull(RuntimeClass.of(String.class).getClassLoader());
		assertSame(RuntimeClassBehaviourTest.class.getClassLoader(),
				RuntimeClass.of(Fixture.class).getClassLoader());
	}

	// --- Object overrides ---

	@Test
	void equals_is_true_for_same_class_and_raw_class_but_false_for_others() {
		RuntimeClass<String> c = RuntimeClass.of(String.class);
		assertEquals(c, RuntimeClass.of(String.class));
		assertTrue(c.equals(String.class));
		assertNotEquals(c, RuntimeClass.of(Integer.class));
		assertFalse(c.equals("not a class"));
	}

	@Test
	void hashCode_and_toString_match_wrapped_class() {
		assertEquals(String.class.hashCode(), RuntimeClass.of(String.class).hashCode());
		assertEquals(String.class.toString(), RuntimeClass.of(String.class).toString());
	}

	// --- Static unwrap helpers ---

	@Test
	void unwrapClasses_maps_each_element_and_handles_null() {
		Class<?>[] raw = RuntimeClass.unwrapClasses(
				new IClass<?>[] { RuntimeClass.of(String.class), RuntimeClass.ofUnchecked(int.class) });
		assertArrayEquals(new Class<?>[] { String.class, int.class }, raw);
		assertEquals(0, RuntimeClass.unwrapClasses(null).length);
	}

	@Test
	void unwrapAnnotationClass_returns_raw_annotation_type() {
		assertSame(Marker.class, RuntimeClass.unwrapAnnotationClass(RuntimeClass.of(Marker.class)));
	}
}
