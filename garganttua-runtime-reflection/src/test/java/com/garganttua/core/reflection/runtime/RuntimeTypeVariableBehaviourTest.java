package com.garganttua.core.reflection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.TypeVariable;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ITypeVariable;

/**
 * Behavioural coverage for {@link RuntimeTypeVariable} — name, bounds and the
 * declaring-element reference are exposed as the mirror reports.
 */
class RuntimeTypeVariableBehaviourTest {

	static class Box<T extends Number> {
	}

	static class Plain<E> {
	}

	@Test
	void getName_matches_declared_variable_name() {
		IClass<Box> box = RuntimeClass.of(Box.class);
		ITypeVariable<?> tv = box.getTypeParameters()[0];
		assertEquals("T", tv.getName());
	}

	@Test
	void getBounds_reflects_extends_clause() {
		IClass<Box> box = RuntimeClass.of(Box.class);
		ITypeVariable<?> tv = box.getTypeParameters()[0];
		assertEquals(1, tv.getBounds().length);
		assertEquals(Number.class, tv.getBounds()[0]);
	}

	@Test
	void unbounded_variable_defaults_to_object_bound() {
		IClass<Plain> plain = RuntimeClass.of(Plain.class);
		ITypeVariable<?> tv = plain.getTypeParameters()[0];
		assertEquals("E", tv.getName());
		assertEquals(Object.class, tv.getBounds()[0]);
	}

	@Test
	void getGenericDeclaration_returns_the_supplied_declaration_mirror() {
		IClass<Box> box = RuntimeClass.of(Box.class);
		ITypeVariable<?> tv = box.getTypeParameters()[0];
		assertSame(box, tv.getGenericDeclaration());
	}

	@Test
	void constructor_directly_wraps_jdk_typevariable() {
		TypeVariable<?> jdk = Box.class.getTypeParameters()[0];
		IClass<Box> declaration = RuntimeClass.of(Box.class);
		RuntimeTypeVariable<IClass<Box>> tv = new RuntimeTypeVariable<>(jdk, declaration);
		assertEquals("T", tv.getName());
		assertEquals(jdk.getTypeName(), tv.getTypeName());
		assertSame(declaration, tv.getGenericDeclaration());
	}

	@Test
	void getTypeName_matches_jdk() {
		TypeVariable<?> jdk = Box.class.getTypeParameters()[0];
		RuntimeTypeVariable<IClass<Box>> tv = new RuntimeTypeVariable<>(jdk, RuntimeClass.of(Box.class));
		assertEquals(jdk.getTypeName(), tv.getTypeName());
		assertTrue(tv.getTypeName().contains("T"));
	}
}
