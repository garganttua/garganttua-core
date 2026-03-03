package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Data;

class CycleDetectionTest {

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
	static class CycleA {
		@FieldMappingRule(sourceFieldAddress = "name")
		private String name;

		@FieldMappingRule(sourceFieldAddress = "ref")
		private CycleB ref;
	}

	@Data
	static class CycleB {
		@FieldMappingRule(sourceFieldAddress = "name")
		private String name;

		@FieldMappingRule(sourceFieldAddress = "ref")
		private CycleA ref;
	}

	@Data
	static class CycleSourceA {
		private String name;
		private CycleSourceB ref;
	}

	@Data
	static class CycleSourceB {
		private String name;
		private CycleSourceA ref;
	}

	@Test
	void testCycleDetectionFailOnCycleTrue() {
		Mapper mapper = new Mapper(reflection);
		mapper.configure(MapperConfigurationItem.FAIL_ON_CYCLE, true);

		CycleSourceA a = new CycleSourceA();
		CycleSourceB b = new CycleSourceB();
		a.setName("A");
		a.setRef(b);
		b.setName("B");
		b.setRef(a); // cycle!

		assertThrows(MapperException.class, () -> {
			mapper.map(a, reflection.getClass(CycleA.class));
		});
	}

	@Test
	void testCycleDetectionFailOnCycleFalse() throws MapperException {
		Mapper mapper = new Mapper(reflection);
		mapper.configure(MapperConfigurationItem.FAIL_ON_CYCLE, false);
		mapper.configure(MapperConfigurationItem.FAIL_ON_ERROR, false);

		CycleSourceA a = new CycleSourceA();
		CycleSourceB b = new CycleSourceB();
		a.setName("A");
		a.setRef(b);
		b.setName("B");
		b.setRef(a); // cycle!

		// Should not throw, returns partial result
		CycleA result = mapper.map(a, reflection.getClass(CycleA.class));
		assertNotNull(result);
		assertEquals("A", result.getName());
	}
}
