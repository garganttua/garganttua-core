package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IField;

/**
 * Behavioural coverage for {@link RuntimeField} — delegation, primitive access,
 * read/write, annotation lookup, caching and the unwrap exception path.
 */
class RuntimeFieldBehaviourTest {

	@Retention(RetentionPolicy.RUNTIME)
	@interface Tag {
		int value();
	}

	@SuppressWarnings("unused")
	static class Holder {
		@Tag(7)
		public int number = 5;
		public boolean flag = true;
		public long big = 100L;
		public double ratio = 1.5;
		private String secret = "hidden";
		public static final int CONST = 42;
	}

	private static IField field(String name) throws Exception {
		return RuntimeField.of(Holder.class.getDeclaredField(name));
	}

	@Test
	void of_caches_per_underlying_field() throws Exception {
		Field f = Holder.class.getDeclaredField("number");
		assertSame(RuntimeField.of(f), RuntimeField.of(f));
	}

	@Test
	void metadata_matches_jdk() throws Exception {
		IField f = field("number");
		assertEquals("number", f.getName());
		assertEquals(int.class.getName(), f.getType().getName());
		assertEquals(Holder.class.getName(), f.getDeclaringClass().getName());
		assertFalse(f.isSynthetic());
	}

	@Test
	void getModifiers_reports_static_final_const() throws Exception {
		int mods = field("CONST").getModifiers();
		assertTrue(Modifier.isStatic(mods));
		assertTrue(Modifier.isFinal(mods));
	}

	@Test
	void get_and_set_object_value_roundtrips() throws Exception {
		IField f = field("number");
		Holder h = new Holder();
		assertEquals(5, f.get(h));
		f.set(h, 99);
		assertEquals(99, h.number);
	}

	@Test
	void primitive_typed_accessors_return_expected_values() throws Exception {
		Holder h = new Holder();
		assertEquals(5, field("number").getInt(h));
		assertTrue(field("flag").getBoolean(h));
		assertEquals(100L, field("big").getLong(h));
		assertEquals(1.5, field("ratio").getDouble(h));
	}

	@Test
	void primitive_typed_setters_mutate_value() throws Exception {
		Holder h = new Holder();
		field("number").setInt(h, 11);
		field("flag").setBoolean(h, false);
		field("big").setLong(h, 222L);
		assertEquals(11, h.number);
		assertFalse(h.flag);
		assertEquals(222L, h.big);
	}

	@Test
	void getInt_on_non_int_field_throws_IllegalArgument() throws Exception {
		IField f = field("flag");
		assertThrows(IllegalArgumentException.class, () -> f.getInt(new Holder()));
	}

	@Test
	void set_with_wrong_type_throws_IllegalArgument() throws Exception {
		IField f = field("number");
		assertThrows(IllegalArgumentException.class, () -> f.set(new Holder(), "not an int"));
	}

	@Test
	void private_field_access_requires_setAccessible() throws Exception {
		IField f = field("secret");
		assertThrows(IllegalAccessException.class, () -> f.get(new Holder()));
		f.setAccessible(true);
		assertEquals("hidden", f.get(new Holder()));
	}

	@Test
	void annotation_lookup_returns_value_and_null_when_absent() throws Exception {
		Tag t = field("number").getAnnotation(RuntimeClass.of(Tag.class));
		assertEquals(7, t.value());
		assertTrue(field("number").isAnnotationPresent(RuntimeClass.of(Tag.class)));
		assertNull(field("flag").getAnnotation(RuntimeClass.of(Tag.class)));
	}

	@Test
	void unwrap_static_throws_for_foreign_ifield() {
		IField foreign = (IField) java.lang.reflect.Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { IField.class },
				(proxy, method, args) -> {
					if ("toString".equals(method.getName())) return "foreignField";
					throw new UnsupportedOperationException(method.getName());
				});
		assertThrows(IllegalArgumentException.class, () -> RuntimeField.unwrap(foreign));
	}

	@Test
	void unwrap_static_returns_field_for_runtime_field() throws Exception {
		Field f = Holder.class.getDeclaredField("number");
		assertEquals(f, RuntimeField.unwrap(RuntimeField.of(f)));
	}

	@Test
	void equals_and_hashCode_track_underlying_field() throws Exception {
		Field f = Holder.class.getDeclaredField("number");
		assertEquals(RuntimeField.of(f), RuntimeField.of(f));
		assertEquals(f.hashCode(), RuntimeField.of(f).hashCode());
		assertFalse(RuntimeField.of(f).equals("string"));
	}
}
