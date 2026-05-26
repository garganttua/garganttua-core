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

	static class PersonSource {
		private String name;
		private int age;
		private String email;
	
	    public PersonSource() { }

	    public String getName() { return this.name; }

	    public int getAge() { return this.age; }

	    public String getEmail() { return this.email; }

	    public void setName(String name) { this.name = name; }

	    public void setAge(int age) { this.age = age; }

	    public void setEmail(String email) { this.email = email; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof PersonSource that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.age, that.age) && java.util.Objects.equals(this.email, that.email);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.age, this.email);
	    }

	    @Override
	    public String toString() {
	        return "PersonSource{" + "name=" + this.name + ", " + "age=" + this.age + ", " + "email=" + this.email + "}";
	    }
	}

	static class PersonDest {
		private String name;
		private int age;
		private String email;
	
	    public PersonDest() { }

	    public String getName() { return this.name; }

	    public int getAge() { return this.age; }

	    public String getEmail() { return this.email; }

	    public void setName(String name) { this.name = name; }

	    public void setAge(int age) { this.age = age; }

	    public void setEmail(String email) { this.email = email; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof PersonDest that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.age, that.age) && java.util.Objects.equals(this.email, that.email);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.age, this.email);
	    }

	    @Override
	    public String toString() {
	        return "PersonDest{" + "name=" + this.name + ", " + "age=" + this.age + ", " + "email=" + this.email + "}";
	    }
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

	static class PartialSource {
		private String name;
		// no 'age' field
	
	    public PartialSource() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof PartialSource that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "PartialSource{" + "name=" + this.name + "}";
	    }
	}

	static class PartialDest {
		private String name;
		private int age;
	
	    public PartialDest() { }

	    public String getName() { return this.name; }

	    public int getAge() { return this.age; }

	    public void setName(String name) { this.name = name; }

	    public void setAge(int age) { this.age = age; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof PartialDest that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.age, that.age);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.age);
	    }

	    @Override
	    public String toString() {
	        return "PartialDest{" + "name=" + this.name + ", " + "age=" + this.age + "}";
	    }
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

	static class IgnoreSource {
		private String name;
		private String secret;
	
	    public IgnoreSource() { }

	    public String getName() { return this.name; }

	    public String getSecret() { return this.secret; }

	    public void setName(String name) { this.name = name; }

	    public void setSecret(String secret) { this.secret = secret; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IgnoreSource that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.secret, that.secret);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.secret);
	    }

	    @Override
	    public String toString() {
	        return "IgnoreSource{" + "name=" + this.name + ", " + "secret=" + this.secret + "}";
	    }
	}

	static class IgnoreDest {
		private String name;

		@MappingIgnore
		private String secret;
	
	    public IgnoreDest() { }

	    public String getName() { return this.name; }

	    public String getSecret() { return this.secret; }

	    public void setName(String name) { this.name = name; }

	    public void setSecret(String secret) { this.secret = secret; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof IgnoreDest that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.secret, that.secret);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.secret);
	    }

	    @Override
	    public String toString() {
	        return "IgnoreDest{" + "name=" + this.name + ", " + "secret=" + this.secret + "}";
	    }
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

	static class StaticTransientSource {
		private String name;
		static String staticField = "static";
		transient String transientField = "transient";

	    public StaticTransientSource() { }

	    public String getName() { return this.name; }

	    public String getTransientField() { return this.transientField; }

	    public void setName(String name) { this.name = name; }

	    public void setTransientField(String transientField) { this.transientField = transientField; }
	}

	static class StaticTransientDest {
		private String name;
		static String staticField = "original";
		transient String transientField = "original";

	    public StaticTransientDest() { }

	    public String getName() { return this.name; }

	    public String getTransientField() { return this.transientField; }

	    public void setName(String name) { this.name = name; }

	    public void setTransientField(String transientField) { this.transientField = transientField; }
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

	static class AnnotatedDest {
		@FieldMappingRule(sourceFieldAddress = "name")
		private String fullName;

		private String email;
	
	    public AnnotatedDest() { }

	    public String getFullName() { return this.fullName; }

	    public String getEmail() { return this.email; }

	    public void setFullName(String fullName) { this.fullName = fullName; }

	    public void setEmail(String email) { this.email = email; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof AnnotatedDest that)) return false;
	        return java.util.Objects.equals(this.fullName, that.fullName) && java.util.Objects.equals(this.email, that.email);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.fullName, this.email);
	    }

	    @Override
	    public String toString() {
	        return "AnnotatedDest{" + "fullName=" + this.fullName + ", " + "email=" + this.email + "}";
	    }
	}

	static class AnnotatedSource {
		private String name;
		private String email;
	
	    public AnnotatedSource() { }

	    public String getName() { return this.name; }

	    public String getEmail() { return this.email; }

	    public void setName(String name) { this.name = name; }

	    public void setEmail(String email) { this.email = email; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof AnnotatedSource that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.email, that.email);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.email);
	    }

	    @Override
	    public String toString() {
	        return "AnnotatedSource{" + "name=" + this.name + ", " + "email=" + this.email + "}";
	    }
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
