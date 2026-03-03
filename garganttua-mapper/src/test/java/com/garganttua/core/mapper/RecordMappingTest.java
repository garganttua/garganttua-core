package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Data;

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

	@Data
	static class PersonPojo {
		private String name;
		private int age;
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

	@Data
	static class SimplePojo {
		private String value;
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

	@Data
	static class PartialPojo {
		private String name;
		// no 'count' or 'active'
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
