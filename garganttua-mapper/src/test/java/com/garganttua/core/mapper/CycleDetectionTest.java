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

	static class CycleA {
		@FieldMappingRule(sourceFieldAddress = "name")
		private String name;

		@FieldMappingRule(sourceFieldAddress = "ref")
		private CycleB ref;
	
	    public CycleA() { }

	    public String getName() { return this.name; }

	    public CycleB getRef() { return this.ref; }

	    public void setName(String name) { this.name = name; }

	    public void setRef(CycleB ref) { this.ref = ref; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof CycleA that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.ref, that.ref);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.ref);
	    }

	    @Override
	    public String toString() {
	        return "CycleA{" + "name=" + this.name + ", " + "ref=" + this.ref + "}";
	    }
	}

	static class CycleB {
		@FieldMappingRule(sourceFieldAddress = "name")
		private String name;

		@FieldMappingRule(sourceFieldAddress = "ref")
		private CycleA ref;
	
	    public CycleB() { }

	    public String getName() { return this.name; }

	    public CycleA getRef() { return this.ref; }

	    public void setName(String name) { this.name = name; }

	    public void setRef(CycleA ref) { this.ref = ref; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof CycleB that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.ref, that.ref);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.ref);
	    }

	    @Override
	    public String toString() {
	        return "CycleB{" + "name=" + this.name + ", " + "ref=" + this.ref + "}";
	    }
	}

	static class CycleSourceA {
		private String name;
		private CycleSourceB ref;
	
	    public CycleSourceA() { }

	    public String getName() { return this.name; }

	    public CycleSourceB getRef() { return this.ref; }

	    public void setName(String name) { this.name = name; }

	    public void setRef(CycleSourceB ref) { this.ref = ref; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof CycleSourceA that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.ref, that.ref);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.ref);
	    }

	    @Override
	    public String toString() {
	        return "CycleSourceA{" + "name=" + this.name + ", " + "ref=" + this.ref + "}";
	    }
	}

	static class CycleSourceB {
		private String name;
		private CycleSourceA ref;
	
	    public CycleSourceB() { }

	    public String getName() { return this.name; }

	    public CycleSourceA getRef() { return this.ref; }

	    public void setName(String name) { this.name = name; }

	    public void setRef(CycleSourceA ref) { this.ref = ref; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof CycleSourceB that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.ref, that.ref);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.ref);
	    }

	    @Override
	    public String toString() {
	        return "CycleSourceB{" + "name=" + this.name + ", " + "ref=" + this.ref + "}";
	    }
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
