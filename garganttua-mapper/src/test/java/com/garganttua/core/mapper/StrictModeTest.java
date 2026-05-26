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

	static class FullSource {
		private String name;
		private int age;
	
	    public FullSource() { }

	    public String getName() { return this.name; }

	    public int getAge() { return this.age; }

	    public void setName(String name) { this.name = name; }

	    public void setAge(int age) { this.age = age; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof FullSource that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.age, that.age);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.age);
	    }

	    @Override
	    public String toString() {
	        return "FullSource{" + "name=" + this.name + ", " + "age=" + this.age + "}";
	    }
	}

	static class FullDest {
		private String name;
		private int age;
	
	    public FullDest() { }

	    public String getName() { return this.name; }

	    public int getAge() { return this.age; }

	    public void setName(String name) { this.name = name; }

	    public void setAge(int age) { this.age = age; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof FullDest that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.age, that.age);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.age);
	    }

	    @Override
	    public String toString() {
	        return "FullDest{" + "name=" + this.name + ", " + "age=" + this.age + "}";
	    }
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

	static class MissingSource {
		private String name;
		// no 'age' or 'email'
	
	    public MissingSource() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MissingSource that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "MissingSource{" + "name=" + this.name + "}";
	    }
	}

	static class MissingDest {
		private String name;
		private int age;
		private String email;
	
	    public MissingDest() { }

	    public String getName() { return this.name; }

	    public int getAge() { return this.age; }

	    public String getEmail() { return this.email; }

	    public void setName(String name) { this.name = name; }

	    public void setAge(int age) { this.age = age; }

	    public void setEmail(String email) { this.email = email; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MissingDest that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.age, that.age) && java.util.Objects.equals(this.email, that.email);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.age, this.email);
	    }

	    @Override
	    public String toString() {
	        return "MissingDest{" + "name=" + this.name + ", " + "age=" + this.age + ", " + "email=" + this.email + "}";
	    }
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

	static class IgnoreSource2 {
		private String name;
	
	    public IgnoreSource2() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IgnoreSource2 that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "IgnoreSource2{" + "name=" + this.name + "}";
	    }
	}

	static class IgnoreDest2 {
		private String name;

		@MappingIgnore
		private String internal;
	
	    public IgnoreDest2() { }

	    public String getName() { return this.name; }

	    public String getInternal() { return this.internal; }

	    public void setName(String name) { this.name = name; }

	    public void setInternal(String internal) { this.internal = internal; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IgnoreDest2 that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.internal, that.internal);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.internal);
	    }

	    @Override
	    public String toString() {
	        return "IgnoreDest2{" + "name=" + this.name + ", " + "internal=" + this.internal + "}";
	    }
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
