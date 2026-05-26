package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.dsl.MappingConfigurationBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class ExecutorCachingTest {

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

	static class CacheSource {
		private String name;
		private int value;
	
	    public CacheSource() { }

	    public String getName() { return this.name; }

	    public int getValue() { return this.value; }

	    public void setName(String name) { this.name = name; }

	    public void setValue(int value) { this.value = value; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof CacheSource that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.value, that.value);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.value);
	    }

	    @Override
	    public String toString() {
	        return "CacheSource{" + "name=" + this.name + ", " + "value=" + this.value + "}";
	    }
	}

	static class CacheDest {
		private String name;
		private int value;
	
	    public CacheDest() { }

	    public String getName() { return this.name; }

	    public int getValue() { return this.value; }

	    public void setName(String name) { this.name = name; }

	    public void setValue(int value) { this.value = value; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof CacheDest that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.value, that.value);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.value);
	    }

	    @Override
	    public String toString() {
	        return "CacheDest{" + "name=" + this.name + ", " + "value=" + this.value + "}";
	    }
	}

	@Test
	void testCacheIsReused() throws MapperException {
		Mapper mapper = new Mapper(reflection);

		CacheSource source1 = new CacheSource();
		source1.setName("first");
		source1.setValue(1);

		CacheSource source2 = new CacheSource();
		source2.setName("second");
		source2.setValue(2);

		CacheDest result1 = mapper.map(source1, reflection.getClass(CacheDest.class));
		CacheDest result2 = mapper.map(source2, reflection.getClass(CacheDest.class));

		assertEquals("first", result1.getName());
		assertEquals("second", result2.getName());

		// Only 1 config should be cached
		assertEquals(1, mapper.mappingConfigurations.size());
	}

	@Test
	void testRegisterWithProgrammaticConfig() throws Exception {
		Mapper mapper = new Mapper(reflection);

		IClass<?> sourceClass = reflection.getClass(CacheSource.class);
		IClass<?> destClass = reflection.getClass(CacheDest.class);

		MappingConfiguration config = MappingConfigurationBuilder.create()
				.from(sourceClass).to(destClass)
				.field("name").to("name").up()
				.field("value").to("value").up()
				.direction(MappingDirection.REGULAR)
				.build();

		mapper.register(config);

		CacheSource source = new CacheSource();
		source.setName("registered");
		source.setValue(42);

		CacheDest dest = (CacheDest) mapper.map(source, destClass);
		assertEquals("registered", dest.getName());
		assertEquals(42, dest.getValue());
	}
}
