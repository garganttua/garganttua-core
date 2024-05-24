package com.garganttua.reflection.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class TestClass extends ParentClass {
	
	private String field;
	
	private InnerClass inner;
	
	private Map<String, InnerClass> map1;
	
	private Map<InnerClass, String> map2;
	
	private Set<InnerClass> set;
	
	private Collection<InnerClass> collection;
	
	private List<InnerClass> list;
	
	private Map<InnerClass, InnerMapValueClass> map3;
	
	private String testMethod(String string) {
		return string;
	}
	
}

@Getter
class ParentClass {
	private boolean parentField;
	private String testMethodInParent(String string) {
		return string;
	}
}

@Getter
class InnerClass {
	
	private long innerField;
	
	public long getInnerField() {
		return this.innerField;
	}
	
	private String testMethodInInner(String string) {
		return string;
	}
}

class InnerMapValueClass {
	
	private long innerFieldInMap;
	
	public long getInnerField() {
		return this.innerFieldInMap;
	}
}


public class GGObjectQueryTest {
	
	@Test
    void testFindFieldInObject() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("field");
		List<Object> field2 = finder.find("map2");
		
		assertNotNull(field);
		assertNotNull(field2);
		
		assertEquals(1, field.size());
		assertEquals(1, field2.size());
		
		GGReflectionException exception = assertThrows(GGReflectionException.class, () -> {
			finder.find("error");
		});
    }
	
	@Test
    void testFindFieldInSuperObject() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("parentField");
		
		assertNotNull(field);
		assertEquals(1, field.size());
    }
	
	@Test
    void testFindFieldInInnerObject() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("parentField");
		
		assertNotNull(field);
		assertEquals(1, field.size());
    }
	
	@Test
    void testFindFieldInList() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("list.innerField");
		
		assertNotNull(field);
		
		assertEquals(2, field.size());
    }
	
	@Test
    void testFindFieldInSet() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("set.innerField");
		
		assertNotNull(field);
		assertEquals(2, field.size());
    }
	
	@Test
    void testFindFieldInCollection() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("collection.innerField");
		
		assertNotNull(field);
		assertEquals(2, field.size());
    }
	
	@Test
    void testFindFieldInMapValue() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("map1.#value.innerField");
		
		assertNotNull(field);
		assertEquals(2, field.size());
		
    }
	
	@Test
    void testFindFieldInMapKey() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		List<Object> field = finder.find("map2.#key.innerField");
		
		assertNotNull(field);
		assertEquals(2, field.size());
    }
	
	@Test
    void testFindFieldInMapwithoutKeyOrValueInAddress() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		
		GGReflectionException exception = assertThrows(GGReflectionException.class, () -> {
			finder.find("map2.innerField");
		});
	
		assertEquals("Field map2 is a map, so address must indicate key or value", exception.getMessage());
    }
	
	@Test
    void testFindMethodInInner() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		
		List<Object> field = finder.find("inner.getInnerField");
		
		assertNotNull(field);
		assertTrue(field.get(1) instanceof Method);
		assertEquals(2, field.size());
    }
	
	
	@Test
    void testFindAddress() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		
		GGObjectAddress address = finder.address("innerField");
		
		assertNotNull(address);
		assertEquals(2, address.length());
    }
	
	@Test
    void testFindAddressInMap() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		
		GGObjectAddress address = finder.address("innerFieldInMap");
		
		assertNotNull(address);
		assertEquals(3, address.length());
    }
	
	@Test
    void testFindAddressInParent() throws GGReflectionException {
		IGGObjectQuery finder = new GGObjectQuery(TestClass.class);
		
		GGObjectAddress address = finder.address("parentField");
		
		assertNotNull(address);
		assertEquals(1, address.length());
    }
	
	@Test
	void testSetValueInParentField() throws GGReflectionException {
		IGGObjectQuery query = new GGObjectQuery(TestClass.class);
		
		TestClass obj = (TestClass) query.setValue("parentField", true);
		assertNotNull(obj);
		assertEquals(true, obj.isParentField());
		
		obj = (TestClass) query.setValue("inner.innerField", 2L);
		assertNotNull(obj);
		assertEquals(2L, obj.getInner().getInnerField());
		
		obj = (TestClass) query.setValue("map1.#value.innerField", List.of(2L));
		assertNotNull(obj);
		assertEquals(1, obj.getMap1().values().size());
	}
	
	@Test
	void testGetFieldValueStructure() throws GGReflectionException {
		IGGObjectQuery query = new GGObjectQuery(TestClass.class);
		
		Object structure = query.fieldValueStructure("field");
		
		assertNotNull(structure);
		assertTrue(String.class.isAssignableFrom(structure.getClass()));
		
	}
	
	@Test
	void testGetFieldValueStructureForAddressWithDepth2() throws GGReflectionException {
		IGGObjectQuery query = new GGObjectQuery(TestClass.class);
		
		Object structure = query.fieldValueStructure("inner.innerField");
		
		assertNotNull(structure);
		assertTrue(Long.class == structure.getClass());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	void testGetFieldValueStructureWithListsForAddressWithDepth3() throws GGReflectionException {
		IGGObjectQuery query = new GGObjectQuery(TestClass.class);
		
		Object structure = query.fieldValueStructure("map2.#key.innerField");
		
		assertNotNull(structure);
		assertTrue(List.class.isAssignableFrom(structure.getClass()));
		
		assertTrue(((List<Long>) structure).get(0).getClass() == Long.class);
	}
	
	@Test
	void testInvokeSimpleMethod() throws GGReflectionException {
		IGGObjectQuery query = new GGObjectQuery(TestClass.class);
		
		String returned = (String) query.invoke("testMethod", "salut");
		
		assertEquals("salut", returned);
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	void testInvokesMethodInList() throws GGReflectionException {
		TestClass c = new TestClass();
		List<InnerClass> list = new ArrayList<InnerClass>();
		list.add(new InnerClass());
		list.add(new InnerClass());
		list.add(new InnerClass());
		c.setList(list);

		IGGObjectQuery query = new GGObjectQuery(c);
		
		List<String> returned = (List<String>) query.invoke("list.testMethodInInner", "salut");
		
		assertEquals("salut", returned.get(0));
		assertEquals("salut", returned.get(1));
		assertEquals("salut", returned.get(2));
	}	
	
}