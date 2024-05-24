package com.garganttua.reflection.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.garganttua.reflection.utils.GGObjectReflectionHelper;

class SuperClass {
	Long superField;
}

class Entity extends SuperClass {
	String field;
	public String aMethod(String test, SuperClass sperClass) {
		return null;
	};
}

public class GGObjectReflectionHelperTest {
	
	
	@Test
	public void testGetField() {
		Field fieldField = GGObjectReflectionHelper.getField(Entity.class, "field");
		
		assertNotNull(fieldField);
		
		Field testField = GGObjectReflectionHelper.getField(Entity.class, "test");
		
		assertNull(testField);
		
		Field superFieldField = GGObjectReflectionHelper.getField(Entity.class, "superField");
		
		assertNotNull(superFieldField);
	}
	
	@Test
	public void testMethodAndParamsChecking() {
		Method method = GGObjectReflectionHelper.getMethod(Entity.class, "aMethod");
		
//		assertThatNoException().isThrownBy(()-> {
//			GGAPIObjectReflectionHelper.checkMethodAndParams(method, "string", new SuperClass());			
//		});
//		
//		assertThatException().isThrownBy(()-> {
//			GGAPIObjectReflectionHelper.checkMethodAndParams(method, 12L, new SuperClass());			
//		});
	}
	

}
