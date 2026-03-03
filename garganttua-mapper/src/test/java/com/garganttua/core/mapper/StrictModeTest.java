package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.MappingIgnore;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Data;

class StrictModeTest {

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
	static class FullSource {
		private String name;
		private int age;
	}

	@Data
	static class FullDest {
		private String name;
		private int age;
	}

	@Test
	void testStrictModeAllMapped() throws MapperException {
		Mapper mapper = new Mapper(reflection);
		mapper.configure(MapperConfigurationItem.STRICT_MODE, true);

		FullSource source = new FullSource();
		source.setName("Alice");
		source.setAge(30);

		FullDest dest = mapper.map(source, reflection.getClass(FullDest.class));
		assertNotNull(dest);
		assertEquals("Alice", dest.getName());
	}

	@Data
	static class MissingSource {
		private String name;
		// no 'age' or 'email'
	}

	@Data
	static class MissingDest {
		private String name;
		private int age;
		private String email;
	}

	@Test
	void testStrictModeUncoveredFieldThrows() {
		Mapper mapper = new Mapper(reflection);
		mapper.configure(MapperConfigurationItem.STRICT_MODE, true);

		MissingSource source = new MissingSource();
		source.setName("Bob");

		MapperException ex = assertThrows(MapperException.class, () -> {
			mapper.map(source, reflection.getClass(MissingDest.class));
		});
		assertTrue(ex.getMessage().contains("uncovered"));
	}

	@Data
	static class IgnoreSource2 {
		private String name;
	}

	@Data
	static class IgnoreDest2 {
		private String name;

		@MappingIgnore
		private String internal;
	}

	@Test
	void testStrictModeWithMappingIgnore() throws MapperException {
		Mapper mapper = new Mapper(reflection);
		mapper.configure(MapperConfigurationItem.STRICT_MODE, true);

		IgnoreSource2 source = new IgnoreSource2();
		source.setName("Charlie");

		// 'internal' is @MappingIgnore so strict mode should pass
		IgnoreDest2 dest = mapper.map(source, reflection.getClass(IgnoreDest2.class));
		assertNotNull(dest);
		assertEquals("Charlie", dest.getName());
		assertNull(dest.getInternal());
	}
}
