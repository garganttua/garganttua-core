package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IRecordComponent;

/**
 * Behavioural coverage for {@link RuntimeRecordComponent} — name, type,
 * accessor wiring, generic type, declaring record and annotation lookup.
 */
class RuntimeRecordComponentBehaviourTest {

	@Retention(RetentionPolicy.RUNTIME)
	@interface Label {
		String value();
	}

	record Person(@Label("first") String firstName, int age, List<String> tags) {
	}

	private static IRecordComponent component(String name) {
		for (RecordComponent rc : Person.class.getRecordComponents()) {
			if (rc.getName().equals(name)) {
				return RuntimeRecordComponent.of(rc);
			}
		}
		throw new IllegalStateException("no component " + name);
	}

	@Test
	void name_and_type_match_declaration() {
		IRecordComponent first = component("firstName");
		assertEquals("firstName", first.getName());
		assertEquals(String.class.getName(), first.getType().getName());

		IRecordComponent age = component("age");
		assertEquals(int.class.getName(), age.getType().getName());
	}

	@Test
	void getDeclaringRecord_points_back_to_record() {
		assertEquals(Person.class.getName(), component("age").getDeclaringRecord().getName());
	}

	@Test
	void getAccessor_returns_matching_method() {
		IMethod accessor = component("age").getAccessor();
		assertEquals("age", accessor.getName());
		assertEquals(int.class.getName(), accessor.getReturnType().getName());
	}

	@Test
	void accessor_invocation_reads_component_value() throws Exception {
		Person p = new Person("Ada", 36, List.of("a"));
		IMethod accessor = component("firstName").getAccessor();
		assertEquals("Ada", accessor.invoke(p));
	}

	@Test
	void getGenericType_carries_generics() {
		assertEquals("java.util.List<java.lang.String>",
				component("tags").getGenericType().getTypeName());
	}

	@Test
	void annotation_lookup_returns_value_and_null_when_absent() {
		Label l = component("firstName").getAnnotation(RuntimeClass.of(Label.class));
		assertEquals("first", l.value());
		assertTrue(component("firstName").isAnnotationPresent(RuntimeClass.of(Label.class)));
		assertNull(component("age").getAnnotation(RuntimeClass.of(Label.class)));
		assertFalse(component("age").isAnnotationPresent(RuntimeClass.of(Label.class)));
	}

	@Test
	void equals_and_hashCode_track_underlying_component() {
		RecordComponent rc = Person.class.getRecordComponents()[0];
		assertEquals(RuntimeRecordComponent.of(rc), RuntimeRecordComponent.of(rc));
		assertEquals(rc.hashCode(), RuntimeRecordComponent.of(rc).hashCode());
		assertFalse(RuntimeRecordComponent.of(rc).equals("x"));
	}

	@Test
	void unwrap_returns_the_wrapped_component() {
		RecordComponent rc = Person.class.getRecordComponents()[0];
		assertEquals(rc, RuntimeRecordComponent.of(rc).unwrap());
	}
}
