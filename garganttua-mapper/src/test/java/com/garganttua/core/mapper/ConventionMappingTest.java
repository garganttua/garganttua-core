package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.MappingIgnore;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Data;

class ConventionMappingTest {

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
	static class PersonSource {
		private String name;
		private int age;
		private String email;
	}

	@Data
	static class PersonDest {
		private String name;
		private int age;
		private String email;
	}

	@Test
	void testFullConventionMapping() throws MapperException {
		PersonSource source = new PersonSource();
		source.setName("Alice");
		source.setAge(30);
		source.setEmail("alice@example.com");

		Mapper mapper = new Mapper(reflection);
		PersonDest dest = mapper.map(source, reflection.getClass(PersonDest.class));

		assertNotNull(dest);
		assertEquals("Alice", dest.getName());
		assertEquals(30, dest.getAge());
		assertEquals("alice@example.com", dest.getEmail());
	}

	@Data
	static class PartialSource {
		private String name;
		// no 'age' field
	}

	@Data
	static class PartialDest {
		private String name;
		private int age;
	}

	@Test
	void testPartialConventionMapping() throws MapperException {
		PartialSource source = new PartialSource();
		source.setName("Bob");

		Mapper mapper = new Mapper(reflection);
		PartialDest dest = mapper.map(source, reflection.getClass(PartialDest.class));

		assertNotNull(dest);
		assertEquals("Bob", dest.getName());
		assertEquals(0, dest.getAge()); // default
	}

	@Data
	static class IgnoreSource {
		private String name;
		private String secret;
	}

	@Data
	static class IgnoreDest {
		private String name;

		@MappingIgnore
		private String secret;
	}

	@Test
	void testMappingIgnoreAnnotation() throws MapperException {
		IgnoreSource source = new IgnoreSource();
		source.setName("Charlie");
		source.setSecret("hidden");

		Mapper mapper = new Mapper(reflection);
		IgnoreDest dest = mapper.map(source, reflection.getClass(IgnoreDest.class));

		assertNotNull(dest);
		assertEquals("Charlie", dest.getName());
		assertNull(dest.getSecret()); // ignored
	}

	@Data
	static class StaticTransientSource {
		private String name;
		static String staticField = "static";
		transient String transientField = "transient";
	}

	@Data
	static class StaticTransientDest {
		private String name;
		static String staticField = "original";
		transient String transientField = "original";
	}

	@Test
	void testStaticAndTransientFieldsIgnored() throws MapperException {
		StaticTransientSource source = new StaticTransientSource();
		source.setName("Dave");

		Mapper mapper = new Mapper(reflection);
		StaticTransientDest dest = mapper.map(source, reflection.getClass(StaticTransientDest.class));

		assertNotNull(dest);
		assertEquals("Dave", dest.getName());
	}

	@Data
	static class AnnotatedDest {
		@FieldMappingRule(sourceFieldAddress = "name")
		private String fullName;

		private String email;
	}

	@Data
	static class AnnotatedSource {
		private String name;
		private String email;
	}

	@Test
	void testConventionComplementsAnnotated() throws MapperException {
		AnnotatedSource source = new AnnotatedSource();
		source.setName("Eve");
		source.setEmail("eve@example.com");

		Mapper mapper = new Mapper(reflection);
		AnnotatedDest dest = mapper.map(source, reflection.getClass(AnnotatedDest.class));

		assertNotNull(dest);
		assertEquals("Eve", dest.getFullName()); // annotated
		assertEquals("eve@example.com", dest.getEmail()); // convention
	}

	@Test
	void testConventionMappingDisabled() throws MapperException {
		PersonSource source = new PersonSource();
		source.setName("Frank");
		source.setAge(25);

		Mapper mapper = new Mapper(reflection);
		mapper.configure(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, false);

		// With convention off and no annotations, should fail
		assertThrows(MapperException.class, () -> {
			mapper.map(source, reflection.getClass(PersonDest.class));
		});
	}
}
