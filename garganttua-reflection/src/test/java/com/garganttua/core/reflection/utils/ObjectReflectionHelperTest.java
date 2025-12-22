package com.garganttua.core.reflection.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

class SuperClass {
	Long superField;

	public void inheritedMethod() {
		// This method will be inherited by Entity
	}

	public void overloadedMethod(String arg) {
		// Overloaded method in super class
	}
}

class Entity extends SuperClass {
	String field;
	public String aMethod(String test, SuperClass sperClass) {
		return null;
	}

	public void overloadedMethod(int arg) {
		// Different signature, not an override
	}
}

public class ObjectReflectionHelperTest {
	
	
	@Test
	public void testGetField() {
		Field fieldField = ObjectReflectionHelper.getField(Entity.class, "field");
		
		assertNotNull(fieldField);
		
		Field testField = ObjectReflectionHelper.getField(Entity.class, "test");
		
		assertNull(testField);
		
		Field superFieldField = ObjectReflectionHelper.getField(Entity.class, "superField");
		
		assertNotNull(superFieldField);
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testMethodAndParamsChecking() {
		Method method = ObjectReflectionHelper.getMethod(Entity.class, "aMethod");

//		assertThatNoException().isThrownBy(()-> {
//			APIObjectReflectionHelper.checkMethodAndParams(method, "string", new SuperClass());
//		});
//
//		assertThatException().isThrownBy(()-> {
//			APIObjectReflectionHelper.checkMethodAndParams(method, 12L, new SuperClass());
//		});
	}

	@Test
	public void testGetMethodsWithInheritance() {
		// Test that getMethods returns inherited method without duplicates
		List<Method> inheritedMethods = ObjectReflectionHelper.getMethods(Entity.class, "inheritedMethod");

		assertNotNull(inheritedMethods);
		assertEquals(1, inheritedMethods.size(), "Should find exactly one inherited method (no duplicates)");
		assertEquals("inheritedMethod", inheritedMethods.get(0).getName());
	}

	@Test
	public void testGetMethodsWithOverloads() {
		// Test that getMethods returns all overloaded methods
		List<Method> overloadedMethods = ObjectReflectionHelper.getMethods(Entity.class, "overloadedMethod");

		assertNotNull(overloadedMethods);
		assertEquals(2, overloadedMethods.size(), "Should find both overloaded methods (String and int versions)");

		// Verify both signatures are present
		boolean hasStringVersion = false;
		boolean hasIntVersion = false;

		for (Method m : overloadedMethods) {
			assertEquals("overloadedMethod", m.getName());
			Class<?>[] paramTypes = m.getParameterTypes();
			assertEquals(1, paramTypes.length);

			if (paramTypes[0] == String.class) {
				hasStringVersion = true;
			} else if (paramTypes[0] == int.class) {
				hasIntVersion = true;
			}
		}

		assertTrue(hasStringVersion, "Should find String version");
		assertTrue(hasIntVersion, "Should find int version");
	}

	@Test
	public void testGetMethodsNoDuplicates() {
		// Test that getMethods doesn't return duplicates for the same signature
		List<Method> methods = ObjectReflectionHelper.getMethods(Entity.class, "inheritedMethod");

		// Count signatures
		java.util.Set<String> signatures = new java.util.HashSet<>();
		for (Method m : methods) {
			StringBuilder sig = new StringBuilder(m.getName());
			sig.append("(");
			for (Class<?> p : m.getParameterTypes()) {
				sig.append(p.getName()).append(",");
			}
			sig.append(")");
			signatures.add(sig.toString());
		}

		assertEquals(methods.size(), signatures.size(), "All methods should have unique signatures");
	}

}
