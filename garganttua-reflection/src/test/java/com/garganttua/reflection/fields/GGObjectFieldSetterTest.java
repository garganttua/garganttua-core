package com.garganttua.reflection.fields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.fields.GGObjectFieldSetter;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.Getter;

public class GGObjectFieldSetterTest {
	
	@Getter
	public static class ObjectTest {
		
		private ObjectTest inner;
		
		private long l;
		
		private String s;
		
		private float f;
		
		private int i;
		
		private Set<ObjectTest> innersInSet;
		
		private List<ObjectTest> innersInList;
		
		private ObjectTest[] innersInArray;
		
		private Map<ObjectTest,ObjectTest> innersInMap;
	}

	@Test
	public void testSetLong() throws GGReflectionException {
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("l");
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);
 
		ObjectTest object = (ObjectTest) setter.setValue(1L);
		
		assertNotNull(object);
		assertEquals(1L, object.getL());
		
	}
	
	@Test
	public void testSetString() throws GGReflectionException {
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "s"));
		
		GGObjectAddress address = new GGObjectAddress("s");
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);
 
		ObjectTest object = (ObjectTest) setter.setValue("test");
		
		assertNotNull(object);
		assertEquals("test", object.getS());
		
	}
	
	@Test
	public void testSetValueInInner() throws GGReflectionException {
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("inner.l");
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);
 
		ObjectTest object = (ObjectTest) setter.setValue(1L);
		
		assertNotNull(object);
		assertEquals(1L, object.getInner().getL());
	}
	
	@Test
	public void testSetValueInInnerWithDepth6() throws GGReflectionException{
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "inner"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("inner.inner.inner.inner.inner.l", false);
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);
 
		ObjectTest object = (ObjectTest) setter.setValue(1L);
		
		assertNotNull(object);
		assertEquals(1L, object.getInner().getInner().getInner().getInner().getInner().getL());
	}
	
	@Test
	public void testSetValuesInList() throws GGReflectionException {
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("innersInList.l");
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);

		ObjectTest object = (ObjectTest) setter.setValue(List.of(1L, 2L, 3L));
		
		assertNotNull(object);
		assertEquals(3, object.getInnersInList().size());
		assertEquals(1L, object.getInnersInList().get(0).getL());
		assertEquals(2L, object.getInnersInList().get(1).getL());
		assertEquals(3L, object.getInnersInList().get(2).getL());
	}
	
	@Test
	public void testSetValuesInListDepth2() throws GGReflectionException {
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("innersInList.innersInList.l", false);
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);

		ObjectTest object = (ObjectTest) setter.setValue(List.of(List.of(1L, 2L, 3L), List.of(1L, 2L, 3L)));
		
		assertNotNull(object);
		assertEquals(2, object.getInnersInList().size());
		assertEquals(1L, object.getInnersInList().get(0).getInnersInList().get(0).getL());
		assertEquals(2L, object.getInnersInList().get(0).getInnersInList().get(1).getL());
		assertEquals(3L, object.getInnersInList().get(0).getInnersInList().get(2).getL());
		assertEquals(1L, object.getInnersInList().get(1).getInnersInList().get(0).getL());
		assertEquals(2L, object.getInnersInList().get(1).getInnersInList().get(1).getL());
		assertEquals(3L, object.getInnersInList().get(1).getInnersInList().get(2).getL());
	}
	
	@Test
	public void testSetValuesInSetDepth2() throws GGReflectionException {
		
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInSet"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("innersInSet.innersInList.l");
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);

		ObjectTest object = (ObjectTest) setter.setValue(List.of(List.of(1L, 2L, 3L), List.of(1L, 2L, 3L)));
		
		assertNotNull(object);
		assertEquals(2, object.getInnersInSet().size());
		object.getInnersInSet().forEach(objectTest -> {
			assertEquals(1L, objectTest.getInnersInList().get(0).getL());
			assertEquals(2L, objectTest.getInnersInList().get(1).getL());
			assertEquals(3L, objectTest.getInnersInList().get(2).getL());
		});
	}
	
	@Test
	public void testSetValuesInArrayDepth2() throws GGReflectionException {
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInArray"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("innersInArray.innersInList.l");
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);
		List<Object> values = List.of(List.of(1L, 2L, 3L), List.of(1L, 2L, 3L));
		
		ObjectTest object = (ObjectTest) setter.setValue(values);
		
		assertNotNull(object);
		assertEquals(2, object.getInnersInArray().length);
		for( ObjectTest objectTest: object.getInnersInArray() ) {
			assertEquals(1L, objectTest.getInnersInList().get(0).getL());
			assertEquals(2L, objectTest.getInnersInList().get(1).getL());
			assertEquals(3L, objectTest.getInnersInList().get(2).getL());
		}
	}
	
	@Test
	public void testSetValuesInMapDepth2() throws GGReflectionException {
		List<Object> fieldInfos = new ArrayList<Object>();
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInMap"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "innersInList"));
		fieldInfos.add(GGObjectReflectionHelper.getField(ObjectTest.class, "l"));
		
		GGObjectAddress address = new GGObjectAddress("innersInMap.#key.innersInList.l");
		
		GGObjectFieldSetter setter = new GGObjectFieldSetter(ObjectTest.class, fieldInfos, address);
		List<Object> values = List.of(List.of(1L, 2L, 3L), List.of(1L, 2L, 3L));
		
		ObjectTest object = (ObjectTest) setter.setValue(values);
		
		assertNotNull(object);
		assertEquals(2, object.getInnersInMap().size());
		object.getInnersInMap().keySet().forEach(objectTest -> {
			assertEquals(1L, objectTest.getInnersInList().get(0).getL());
			assertEquals(2L, objectTest.getInnersInList().get(1).getL());
			assertEquals(3L, objectTest.getInnersInList().get(2).getL());
		});
	}
}


