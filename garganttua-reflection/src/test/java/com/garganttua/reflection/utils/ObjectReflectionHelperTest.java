package com.garganttua.reflection.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class SuperClass {
	Long superField;
}

class Entity extends SuperClass {
	String field;
	public String aMethod(String test, SuperClass sperClass) {
		return null;
	};
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
	

}
