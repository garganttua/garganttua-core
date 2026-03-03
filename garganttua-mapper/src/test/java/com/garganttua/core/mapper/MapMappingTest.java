package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Data;

class MapMappingTest {

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
	static class MapSourceSame {
		private Map<String, Integer> data;
	}

	@Data
	static class MapDestSame {
		private Map<String, Integer> data;
	}

	@Test
	void testSameTypeMapMapping() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		MapSourceSame source = new MapSourceSame();
		Map<String, Integer> data = new HashMap<>();
		data.put("a", 1);
		data.put("b", 2);
		source.setData(data);

		MapDestSame dest = mapper.map(source, reflection.getClass(MapDestSame.class));
		assertNotNull(dest);
		assertNotNull(dest.getData());
		assertEquals(2, dest.getData().size());
		assertEquals(1, dest.getData().get("a"));
		assertEquals(2, dest.getData().get("b"));
	}

	@Data
	static class ValueA {
		private String name;
	}

	@Data
	static class ValueB {
		private String name;
	}

	@Data
	static class MapSourceMappable {
		private Map<String, ValueA> items;
	}

	@Data
	static class MapDestMappable {
		@FieldMappingRule(sourceFieldAddress = "items")
		private Map<String, ValueB> items;
	}

	@Test
	void testMappableValueMapMapping() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		MapSourceMappable source = new MapSourceMappable();
		Map<String, ValueA> items = new HashMap<>();
		ValueA va = new ValueA();
		va.setName("hello");
		items.put("key1", va);
		source.setItems(items);

		MapDestMappable dest = mapper.map(source, reflection.getClass(MapDestMappable.class));
		assertNotNull(dest);
		assertNotNull(dest.getItems());
		assertEquals(1, dest.getItems().size());
		assertEquals("hello", dest.getItems().get("key1").getName());
	}
}
