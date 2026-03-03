package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.mapper.dsl.MappingConfigurationBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Data;

class ProgrammaticBuilderTest {

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
	static class BuilderSource {
		private String name;
		private int age;
	}

	@Data
	static class BuilderDest {
		private String fullName;
		private int years;
	}

	@Test
	void testBuilderBasic() throws Exception {
		IClass<?> sourceClass = reflection.getClass(BuilderSource.class);
		IClass<?> destClass = reflection.getClass(BuilderDest.class);

		MappingConfiguration config = MappingConfigurationBuilder.create()
				.from(sourceClass).to(destClass)
				.field("name").to("fullName").up()
				.field("age").to("years").up()
				.direction(MappingDirection.REGULAR)
				.build();

		assertNotNull(config);
		assertEquals(2, config.destinationRules().size());
		assertEquals(MappingDirection.REGULAR, config.mappingDirection());
	}

	@Test
	void testBuilderAndRegister() throws Exception {
		Mapper mapper = new Mapper(reflection);

		IClass<?> sourceClass = reflection.getClass(BuilderSource.class);
		IClass<?> destClass = reflection.getClass(BuilderDest.class);

		MappingConfiguration config = MappingConfigurationBuilder.create()
				.from(sourceClass).to(destClass)
				.field("name").to("fullName").up()
				.field("age").to("years").up()
				.build();

		mapper.register(config);

		BuilderSource source = new BuilderSource();
		source.setName("Alice");
		source.setAge(30);

		BuilderDest dest = (BuilderDest) mapper.map(source, destClass);
		assertNotNull(dest);
		assertEquals("Alice", dest.getFullName());
		assertEquals(30, dest.getYears());
	}

	@Test
	void testBuilderMissingSourceThrows() {
		assertThrows(DslException.class, () -> {
			MappingConfigurationBuilder.create()
					.to(reflection.getClass(BuilderDest.class))
					.build();
		});
	}

	@Test
	void testBuilderMissingDestinationThrows() {
		assertThrows(DslException.class, () -> {
			MappingConfigurationBuilder.create()
					.from(reflection.getClass(BuilderSource.class))
					.build();
		});
	}
}
