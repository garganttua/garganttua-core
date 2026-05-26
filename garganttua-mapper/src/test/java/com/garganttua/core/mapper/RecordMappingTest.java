package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class RecordMappingTest {

	private static IReflection reflection;

	@BeforeAll
	static void setUp() throws Exception {
		reflection = ReflectionBuilder.builder()
				.withProvider(new RuntimeReflectionProvider())
				.build();
		IClass.setReflection(reflection);
	}

	@AfterAll
	static void tearDown() {
		IClass.setReflection(null);
	}

	static class PersonPojo {
		private String name;
		private int age;
	
	    public PersonPojo() { }

	    public String getName() { return this.name; }

	    public int getAge() { return this.age; }

	    public void setName(String name) { this.name = name; }

	    public void setAge(int age) { this.age = age; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof PersonPojo that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.age, that.age);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.age);
	    }

	    @Override
	    public String toString() {
	        return "PersonPojo{" + "name=" + this.name + ", " + "age=" + this.age + "}";
	    }
	}

	record PersonRecord(String name, int age) {}

	@Test
	void testPojoToRecord() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		PersonPojo source = new PersonPojo();
		source.setName("Alice");
		source.setAge(30);

		PersonRecord result = mapper.map(source, reflection.getClass(PersonRecord.class));
		assertNotNull(result);
		assertEquals("Alice", result.name());
		assertEquals(30, result.age());
	}

	record SimpleRecord(String value) {}

	static class SimplePojo {
		private String value;
	
	    public SimplePojo() { }

	    public String getValue() { return this.value; }

	    public void setValue(String value) { this.value = value; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof SimplePojo that)) return false;
	        return java.util.Objects.equals(this.value, that.value);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.value);
	    }

	    @Override
	    public String toString() {
	        return "SimplePojo{" + "value=" + this.value + "}";
	    }
	}

	@Test
	void testPojoToSimpleRecord() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		SimplePojo source = new SimplePojo();
		source.setValue("hello");

		SimpleRecord result = mapper.map(source, reflection.getClass(SimpleRecord.class));
		assertNotNull(result);
		assertEquals("hello", result.value());
	}

	record DefaultsRecord(String name, int count, boolean active) {}

	static class PartialPojo {
		private String name;
		// no 'count' or 'active'
	
	    public PartialPojo() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof PartialPojo that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "PartialPojo{" + "name=" + this.name + "}";
	    }
	}

	@Test
	void testRecordWithDefaultValues() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		PartialPojo source = new PartialPojo();
		source.setName("Bob");

		DefaultsRecord result = mapper.map(source, reflection.getClass(DefaultsRecord.class));
		assertNotNull(result);
		assertEquals("Bob", result.name());
		assertEquals(0, result.count()); // default
		assertFalse(result.active()); // default
	}
}
