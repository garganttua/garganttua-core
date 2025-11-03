package com.garganttua.core.reflection.fields;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.AllArgsConstructor;

public class ObjectFieldGetterTest {
	
	@AllArgsConstructor
	class ObjectTest {
		
		@SuppressWarnings("unused")
		private ObjectTest inner;
		
		@SuppressWarnings("unused")
		private long l;
		
		@SuppressWarnings("unused")
		private String s;
		
		@SuppressWarnings("unused")
		private float f;
		
		@SuppressWarnings("unused")
		private int i;
		
		@SuppressWarnings("unused")
		private List<ObjectTest> innersInList;
		
		@SuppressWarnings("unused")
		private ObjectTest[] innersInArray;
		
		@SuppressWarnings("unused")
		private Map<ObjectTest,ObjectTest> innersInMap;
	}
	
	private ObjectTest createNestedObject(int depth) {
        if (depth <= 0) {
            return null;
        }

        ObjectTest inner = createNestedObject(depth - 1);
        List<ObjectTest> innerList = new ArrayList<>();
        innerList.add(createNestedObject(depth - 1));

        ObjectTest[] innerArray = new ObjectTest[1];
        innerArray[0] = createNestedObject(depth - 1);

        Map<ObjectTest, ObjectTest> innerMap = new HashMap<>();
        innerMap.put(createNestedObject(depth - 1), createNestedObject(depth - 1));

        return new ObjectTest(inner, depth, "Depth-" + depth, depth, depth, innerList, innerArray, innerMap);
    }
	
	@Test
	public void testGetValueOfField() throws ReflectionException {
		
		ObjectTest o = this.createNestedObject(1);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		ObjectAddress address = new ObjectAddress("l");
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address );
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1L, value);
	}
	
	@Test
	public void testGetValueOfFieldOfInner() throws ReflectionException {
		
		ObjectTest o = this.createNestedObject(2);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "f"));
		
		ObjectAddress address = new ObjectAddress("inner.f");
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1F, value);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetValueOfFieldOfInnerInList() throws ReflectionException {
		
		ObjectTest o = this.createNestedObject(2);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.f");
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertTrue(List.class.isAssignableFrom(value.getClass()));
		assertEquals(1, ((List<Object>) value).get(0));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetValueOfFieldOfInnerInKeyOfMap() throws ReflectionException {
		
		ObjectTest o = this.createNestedObject(2);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInMap"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("innersMap.#key.i");
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertTrue(List.class.isAssignableFrom(value.getClass()));
		assertTrue(((List<Object>) value).contains(1));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetValueOfFieldOfInnerInValueOfMap() throws ReflectionException {
		
		ObjectTest o = this.createNestedObject(2);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInMap"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("innersMap.#value.i");
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertTrue(List.class.isAssignableFrom(value.getClass()));
		assertTrue(((List<Object>) value).contains(1));
	}
	
	@Test
	public void testGetValueOfFieldOfInnerInInner() throws ReflectionException {

		ObjectTest o = this.createNestedObject(3);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.inner.i", false);
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1, value);
	}
	
	@Test
	public void testGetValueOfFieldDepth6() throws ReflectionException {

		ObjectTest o = this.createNestedObject(6);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.inner.inner.inner.inner.i", false);
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1, value);
		
		List<Object> fieldInfos2 = new ArrayList<Object>();
		fieldInfos2.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos2.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos2.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos2.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address2 = new ObjectAddress("inner.inner.inner.i", false);
		ObjectFieldGetter getter2 = new ObjectFieldGetter(ObjectTest.class, fieldInfos2, address2);
		
		Object value2 = getter2.getValue(o);
		assertNotNull(value2);
		assertEquals(3, value2);
	}
	
	@Test
	public void testGetValueOfFieldInInnerMapDepth6() throws ReflectionException {

		ObjectTest o = this.createNestedObject(6);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.innerInMap.#key.inner.inner.inner.i", false);
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1, value);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetValueOfFieldInListDepth6() throws ReflectionException {

		ObjectTest o = this.createNestedObject(6);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.inner.innersInList.inner.inner.i", false);
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1, ((List<Object>) value).get(0));
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetValueOfFieldInMapDepth6() throws ReflectionException {

		ObjectTest o = this.createNestedObject(6);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInMap"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.inner.inner.innersInMap.#key.inner.i", false);
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1, ((List<Object>) value).get(0));
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetValueDepth6ComplexInner() throws ReflectionException {

		ObjectTest o = this.createNestedObject(6);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInArray"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "innersInMap"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.innersInList.innersInArray.innersInMap.#value.inner.i", false);
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNotNull(value);
		assertEquals(1, ((List<Object>) ((List<Object>) ((List<Object>) value).get(0)).get(0)).get(0));
		
	}

	@Test
	public void testGetNullValue() throws ReflectionException {

		ObjectTest o = new ObjectTest(null, 0, null, 0, 0, null, null, null);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		
		ObjectAddress address = new ObjectAddress("inner");
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNull(value);
	}
	

	@Test
	public void testGetNullValueFromNullInner() throws ReflectionException {

		ObjectTest o = new ObjectTest(null, 0, null, 0, 0, null, null, null);
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(ObjectReflectionHelper.getField(ObjectTest.class, "i"));
		
		ObjectAddress address = new ObjectAddress("inner.i");
		ObjectFieldGetter getter = new ObjectFieldGetter(ObjectTest.class, fieldInfos, address);
		
		Object value = getter.getValue(o);
		assertNull(value);
	}

}
