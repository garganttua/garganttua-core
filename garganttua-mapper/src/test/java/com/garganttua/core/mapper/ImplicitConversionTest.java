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

class ImplicitConversionTest {

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

	enum Color { RED, GREEN, BLUE }

	@Data
	static class EnumSource {
		private String color;
	}

	@Data
	static class EnumDest {
		private Color color;
	}

	@Test
	void testStringToEnum() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		EnumSource source = new EnumSource();
		source.setColor("RED");

		EnumDest dest = mapper.map(source, reflection.getClass(EnumDest.class));
		assertNotNull(dest);
		assertEquals(Color.RED, dest.getColor());
	}

	@Data
	static class EnumSource2 {
		private Color color;
	}

	@Data
	static class EnumDest2 {
		private String color;
	}

	@Test
	void testEnumToString() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		EnumSource2 source = new EnumSource2();
		source.setColor(Color.BLUE);

		EnumDest2 dest = mapper.map(source, reflection.getClass(EnumDest2.class));
		assertNotNull(dest);
		assertEquals("BLUE", dest.getColor());
	}

	@Data
	static class IntSource {
		private String value;
	}

	@Data
	static class IntDest {
		private int value;
	}

	@Test
	void testStringToInt() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		IntSource source = new IntSource();
		source.setValue("42");

		IntDest dest = mapper.map(source, reflection.getClass(IntDest.class));
		assertNotNull(dest);
		assertEquals(42, dest.getValue());
	}

	@Data
	static class BoolSource {
		private String active;
	}

	@Data
	static class BoolDest {
		private boolean active;
	}

	@Test
	void testStringToBoolean() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		BoolSource source = new BoolSource();
		source.setActive("true");

		BoolDest dest = mapper.map(source, reflection.getClass(BoolDest.class));
		assertNotNull(dest);
		assertTrue(dest.isActive());
	}

	@Data
	static class IntToStrSource {
		private int count;
	}

	@Data
	static class IntToStrDest {
		private String count;
	}

	@Test
	void testIntToString() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		IntToStrSource source = new IntToStrSource();
		source.setCount(99);

		IntToStrDest dest = mapper.map(source, reflection.getClass(IntToStrDest.class));
		assertNotNull(dest);
		assertEquals("99", dest.getCount());
	}
}
