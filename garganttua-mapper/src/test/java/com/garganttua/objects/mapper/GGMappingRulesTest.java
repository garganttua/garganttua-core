package com.garganttua.objects.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.objects.mapper.annotations.GGFieldMappingRule;
import com.garganttua.objects.mapper.annotations.GGObjectMappingRule;
import com.garganttua.objects.mapper.rules.GGMappingRule;
import com.garganttua.objects.mapper.rules.GGMappingRules;

class Parent {
	@GGFieldMappingRule(sourceFieldAddress = "parent")
	private String parent;
}

class Inner {
	@GGFieldMappingRule(sourceFieldAddress = "inner")
	private String inner;
}

@GGObjectMappingRule(fromSourceMethod = "from", toSourceMethod = "to")
class Inner2 {
	@GGFieldMappingRule(sourceFieldAddress = "inner")
	private String inner;
	
	public void from() {
		
	}
	public void to() {
		
	}
}

class Destination extends Parent {
	@GGFieldMappingRule(sourceFieldAddress = "field")
	private String field;
	
	private Inner inner;
	
	private List<Inner> list;
	
	private Map<String, Inner> map1;
	
	private Map<Inner, String> map2;
	
	private Set<Inner> set;
	
	private Collection<Inner> collection;
}

@GGObjectMappingRule(fromSourceMethod = "from", toSourceMethod = "to")
class Destination2 extends Parent {
	
	@GGFieldMappingRule(sourceFieldAddress = "field")
	private String field;
	
	private List<Inner2> list;
	
	public void from() {
		
	}
	public void to() {
		
	}

}

class DestinationWithMappingfromFieldThatDoesntExist{
	
	@GGFieldMappingRule(sourceFieldAddress = "notExists")
	private String field;

}

class Source {
	private int field;
}

class DestinationWithIncorrectFromMethod{
	
	@GGFieldMappingRule(sourceFieldAddress = "field", fromSourceMethod = "from")
	private String field;
	
	public String from(String field) {
		return "";
	}
}

class DestinationWithIncorrectToMethod{
	@GGFieldMappingRule(sourceFieldAddress = "field", toSourceMethod = "to")
	private String field;
	
	public String to(String field) {
		return "";
	}
}

class DestinationWithNoToMethod{
	@GGFieldMappingRule(sourceFieldAddress = "field", toSourceMethod = "to")
	private String field;

}

class CorrectDestination {
	
	@GGFieldMappingRule(sourceFieldAddress = "field", fromSourceMethod = "from", toSourceMethod = "to")
	private String field;
	
	public String from(int field) {
		return "";
		
	}
	public int to(String field) {
		return 1;
	}
}

class GGMappingRulesTest {
	
	@Test
	void testmappingRuleOnField() throws GGMapperException {
		
		List<GGMappingRule> rules = GGMappingRules.parse(Destination.class);
		
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
	public void testMappingRuleOnObject() throws GGMapperException {
		List<GGMappingRule> rules = GGMappingRules.parse(Destination2.class);
		
		assertEquals(3, rules.size());
	}
	
	@Test
	public void testValidate() throws GGMapperException {
		List<GGMappingRule> rules = GGMappingRules.parse(CorrectDestination.class);
		assertEquals(1, rules.size());
		
		assertDoesNotThrow(() -> {
			GGMappingRules.validate(Source.class, rules);
		});
		
		List<GGMappingRule> rules2 = GGMappingRules.parse(DestinationWithMappingfromFieldThatDoesntExist.class);
		assertEquals(1, rules2.size());

		GGMapperException exception = assertThrows( GGMapperException.class , () -> GGMappingRules.validate(Destination2.class, rules2));
		
		assertNotNull(exception);
		assertEquals("com.garganttua.reflection.GGReflectionException: Object element notExists not found in class class com.garganttua.objects.mapper.Parent", exception.getMessage());
	
		List<GGMappingRule> rules3 = GGMappingRules.parse(DestinationWithIncorrectFromMethod.class);
		assertEquals(1, rules3.size());
	
		GGMapperException exception2 = assertThrows( GGMapperException.class , () -> GGMappingRules.validate(Source.class, rules3));
		
		assertNotNull(exception2);
		assertEquals("Invalid method from of class DestinationWithIncorrectFromMethod : parameter must be of type int", exception2.getMessage());

		List<GGMappingRule> rules4 = GGMappingRules.parse(DestinationWithIncorrectToMethod.class);
		assertEquals(1, rules4.size());
	
		GGMapperException exception3 = assertThrows( GGMapperException.class , () -> GGMappingRules.validate(Source.class, rules4));
		
		assertNotNull(exception3);
		assertEquals("Invalid method to of class DestinationWithIncorrectToMethod : return type must be int", exception3.getMessage());
		
		List<GGMappingRule> rules5 = GGMappingRules.parse(DestinationWithNoToMethod.class);
		assertEquals(1, rules5.size());
	
		GGMapperException exception4 = assertThrows( GGMapperException.class , () -> GGMappingRules.validate(Source.class, rules5));
		
		assertNotNull(exception4);
		assertEquals("com.garganttua.reflection.GGReflectionException: Object element to not found in class class com.garganttua.objects.mapper.DestinationWithNoToMethod", exception4.getMessage());
		
	
	}
	
}
