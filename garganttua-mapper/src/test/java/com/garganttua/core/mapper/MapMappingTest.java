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

	static class MapSourceSame {
		private Map<String, Integer> data;
	
	    public MapSourceSame() { }

	    public Map<String, Integer> getData() { return this.data; }

	    public void setData(Map<String, Integer> data) { this.data = data; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MapSourceSame that)) return false;
	        return java.util.Objects.equals(this.data, that.data);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.data);
	    }

	    @Override
	    public String toString() {
	        return "MapSourceSame{" + "data=" + this.data + "}";
	    }
	}

	static class MapDestSame {
		private Map<String, Integer> data;
	
	    public MapDestSame() { }

	    public Map<String, Integer> getData() { return this.data; }

	    public void setData(Map<String, Integer> data) { this.data = data; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MapDestSame that)) return false;
	        return java.util.Objects.equals(this.data, that.data);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.data);
	    }

	    @Override
	    public String toString() {
	        return "MapDestSame{" + "data=" + this.data + "}";
	    }
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

	static class ValueA {
		private String name;
	
	    public ValueA() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof ValueA that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "ValueA{" + "name=" + this.name + "}";
	    }
	}

	static class ValueB {
		private String name;
	
	    public ValueB() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof ValueB that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "ValueB{" + "name=" + this.name + "}";
	    }
	}

	static class MapSourceMappable {
		private Map<String, ValueA> items;
	
	    public MapSourceMappable() { }

	    public Map<String, ValueA> getItems() { return this.items; }

	    public void setItems(Map<String, ValueA> items) { this.items = items; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MapSourceMappable that)) return false;
	        return java.util.Objects.equals(this.items, that.items);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.items);
	    }

	    @Override
	    public String toString() {
	        return "MapSourceMappable{" + "items=" + this.items + "}";
	    }
	}

	static class MapDestMappable {
		@FieldMappingRule(sourceFieldAddress = "items")
		private Map<String, ValueB> items;
	
	    public MapDestMappable() { }

	    public Map<String, ValueB> getItems() { return this.items; }

	    public void setItems(Map<String, ValueB> items) { this.items = items; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MapDestMappable that)) return false;
	        return java.util.Objects.equals(this.items, that.items);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.items);
	    }

	    @Override
	    public String toString() {
	        return "MapDestMappable{" + "items=" + this.items + "}";
	    }
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
