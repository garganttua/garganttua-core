package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.ObjectMappingRule;

class Parent {
	@FieldMappingRule(sourceFieldAddress = "parent")
	private String parent;
}

class Inner {
	@FieldMappingRule(sourceFieldAddress = "inner")
	private String inner;
}

@ObjectMappingRule(fromSourceMethod = "from", toSourceMethod = "to")
class Inner2 {
	@FieldMappingRule(sourceFieldAddress = "inner")
	private String inner;
	
	public void from() {
		
	}
	public void to() {
		
	}
}

class Destination extends Parent {
	@FieldMappingRule(sourceFieldAddress = "field")
	private String field;
	
	@SuppressWarnings("unused")
	private Inner inner;
	
	@SuppressWarnings("unused")
	private List<Inner> list;
	
	@SuppressWarnings("unused")
	private Map<String, Inner> map1;
	
	@SuppressWarnings("unused")
	private Map<Inner, String> map2;
	
	@SuppressWarnings("unused")
	private Set<Inner> set;
	
	@SuppressWarnings("unused")
	private Collection<Inner> collection;
}

@ObjectMappingRule(fromSourceMethod = "from", toSourceMethod = "to")
class Destination2 extends Parent {
	
	@FieldMappingRule(sourceFieldAddress = "field")
	private String field;
	
	@SuppressWarnings("unused")
	private List<Inner2> list;
	
	public void from() {
		
	}
	public void to() {
		
	}

}

class DestinationWithMappingfromFieldThatDoesntExist{
	
	@FieldMappingRule(sourceFieldAddress = "notExists")
	private String field;

}

class Source {
	@SuppressWarnings("unused")
	private int field;
}

class DestinationWithIncorrectFromMethod{
	
	@FieldMappingRule(sourceFieldAddress = "field", fromSourceMethod = "from")
	private String field;
	
	public String from(String field) {
		return "";
	}
}

class DestinationWithIncorrectToMethod{
	@FieldMappingRule(sourceFieldAddress = "field", toSourceMethod = "to")
	private String field;
	
	public String to(String field) {
		return "";
	}
}

class DestinationWithNoToMethod{
	@FieldMappingRule(sourceFieldAddress = "field", toSourceMethod = "to")
	private String field;

}

class CorrectDestination {
	
	@FieldMappingRule(sourceFieldAddress = "field", fromSourceMethod = "from", toSourceMethod = "to")
	private String field;
	
	public String from(int field) {
		return "";
		
	}
	public int to(String field) {
		return 1;
	}
}

class MappingRulesTest {
	
	@Test
	void testmappingRuleOnField() throws MapperException {
		
		List<MappingRule> rules = MappingRules.parse(Destination.class);
		
		assertEquals(7, rules.size());
		
		assertEquals("field", rules.get(0).sourceFieldAddress().toString());
		assertEquals("field", rules.get(0).destinationFieldAddress().toString());
		assertNull(rules.get(0).fromSourceMethodAddress());
		assertEquals(Destination.class, rules.get(0).destinationClass());
		
		assertEquals("inner", rules.get(1).sourceFieldAddress().toString());
		assertEquals("list.inner", rules.get(1).destinationFieldAddress().toString());
		assertNull(rules.get(1).fromSourceMethodAddress());
		assertEquals(Inner.class, rules.get(1).destinationClass());
		
		assertEquals("inner", rules.get(2).sourceFieldAddress().toString());
		assertEquals("map1.#value.inner", rules.get(2).destinationFieldAddress().toString());
		assertNull(rules.get(2).fromSourceMethodAddress());
		assertEquals(Inner.class, rules.get(2).destinationClass());
		
		assertEquals("inner", rules.get(3).sourceFieldAddress().toString());
		assertEquals("map2.#key.inner", rules.get(3).destinationFieldAddress().toString());
		assertNull(rules.get(3).fromSourceMethodAddress());
		assertEquals(Inner.class, rules.get(3).destinationClass());
		
		assertEquals("inner", rules.get(4).sourceFieldAddress().toString());
		assertEquals("set.inner", rules.get(4).destinationFieldAddress().toString());
		assertNull(rules.get(4).fromSourceMethodAddress());
		assertEquals(Inner.class, rules.get(4).destinationClass());
		
		assertEquals("inner", rules.get(5).sourceFieldAddress().toString());
		assertEquals("collection.inner", rules.get(5).destinationFieldAddress().toString());
		assertNull(rules.get(5).fromSourceMethodAddress());
		assertEquals(Inner.class, rules.get(5).destinationClass());
		
		assertEquals("parent", rules.get(6).sourceFieldAddress().toString());
		assertEquals("parent", rules.get(6).destinationFieldAddress().toString());
		assertNull(rules.get(6).fromSourceMethodAddress());
		assertEquals(Parent.class, rules.get(6).destinationClass());
	}
	
	@Test
	public void testMappingRuleOnObject() throws MapperException {
		List<MappingRule> rules = MappingRules.parse(Destination2.class);
		
		assertEquals(3, rules.size());
	}
	
	@Test
	public void testValidate() throws MapperException {
		List<MappingRule> rules = MappingRules.parse(CorrectDestination.class);
		assertEquals(1, rules.size());
		
		assertDoesNotThrow(() -> {
			MappingRules.validate(Source.class, rules);
		});
		
		List<MappingRule> rules2 = MappingRules.parse(DestinationWithMappingfromFieldThatDoesntExist.class);
		assertEquals(1, rules2.size());

		MapperException exception = assertThrows( MapperException.class , () -> MappingRules.validate(Destination2.class, rules2));
		
		assertNotNull(exception);
		assertEquals("com.garganttua.core.reflection.ReflectionException: Object element notExists not found in class class com.garganttua.core.mapper.Parent", exception.getMessage());
	
		List<MappingRule> rules3 = MappingRules.parse(DestinationWithIncorrectFromMethod.class);
		assertEquals(1, rules3.size());
	
		MapperException exception2 = assertThrows( MapperException.class , () -> MappingRules.validate(Source.class, rules3));
		
		assertNotNull(exception2);
		assertEquals("Invalid method from of class DestinationWithIncorrectFromMethod : parameter must be of type int", exception2.getMessage());

		List<MappingRule> rules4 = MappingRules.parse(DestinationWithIncorrectToMethod.class);
		assertEquals(1, rules4.size());
	
		MapperException exception3 = assertThrows( MapperException.class , () -> MappingRules.validate(Source.class, rules4));
		
		assertNotNull(exception3);
		assertEquals("Invalid method to of class DestinationWithIncorrectToMethod : return type must be int", exception3.getMessage());
		
		List<MappingRule> rules5 = MappingRules.parse(DestinationWithNoToMethod.class);
		assertEquals(1, rules5.size());
	
		MapperException exception4 = assertThrows( MapperException.class , () -> MappingRules.validate(Source.class, rules5));
		
		assertNotNull(exception4);
		assertEquals("com.garganttua.core.reflection.ReflectionException: Object element to not found in class class com.garganttua.core.mapper.DestinationWithNoToMethod", exception4.getMessage());
		
	
	}
	
}
