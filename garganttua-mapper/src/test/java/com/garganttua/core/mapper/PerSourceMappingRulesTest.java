package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.ObjectMappingRule;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import java.util.List;

public class PerSourceMappingRulesTest {

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

	// --- Fixtures ---------------------------------------------------------

	public static class EntityA {
		private String firstName;
		private String email;
	
	    public EntityA() { }

	    public String getFirstName() { return this.firstName; }

	    public String getEmail() { return this.email; }

	    public void setFirstName(String firstName) { this.firstName = firstName; }

	    public void setEmail(String email) { this.email = email; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof EntityA that)) return false;
	        return java.util.Objects.equals(this.firstName, that.firstName) && java.util.Objects.equals(this.email, that.email);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.firstName, this.email);
	    }

	    @Override
	    public String toString() {
	        return "EntityA{" + "firstName=" + this.firstName + ", " + "email=" + this.email + "}";
	    }
	}

	public static class EntityB {
		private String prenom;
		private String courriel;
	
	    public EntityB() { }

	    public String getPrenom() { return this.prenom; }

	    public String getCourriel() { return this.courriel; }

	    public void setPrenom(String prenom) { this.prenom = prenom; }

	    public void setCourriel(String courriel) { this.courriel = courriel; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof EntityB that)) return false;
	        return java.util.Objects.equals(this.prenom, that.prenom) && java.util.Objects.equals(this.courriel, that.courriel);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.prenom, this.courriel);
	    }

	    @Override
	    public String toString() {
	        return "EntityB{" + "prenom=" + this.prenom + ", " + "courriel=" + this.courriel + "}";
	    }
	}

	public static class MultiSourceDto {
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "firstName")
		@FieldMappingRule(source = EntityB.class, sourceFieldAddress = "prenom")
		private String name;

		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "email")
		@FieldMappingRule(source = EntityB.class, sourceFieldAddress = "courriel")
		private String contact;
	
	    public MultiSourceDto() { }

	    public String getName() { return this.name; }

	    public String getContact() { return this.contact; }

	    public void setName(String name) { this.name = name; }

	    public void setContact(String contact) { this.contact = contact; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MultiSourceDto that)) return false;
	        return java.util.Objects.equals(this.name, that.name) && java.util.Objects.equals(this.contact, that.contact);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name, this.contact);
	    }

	    @Override
	    public String toString() {
	        return "MultiSourceDto{" + "name=" + this.name + ", " + "contact=" + this.contact + "}";
	    }
	}

	public static class WildcardOverrideDto {
		@FieldMappingRule(sourceFieldAddress = "firstName")
		@FieldMappingRule(source = EntityB.class, sourceFieldAddress = "prenom")
		private String name;
	
	    public WildcardOverrideDto() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof WildcardOverrideDto that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "WildcardOverrideDto{" + "name=" + this.name + "}";
	    }
	}

	public static class AbstractEntity {
		protected String name;
	
	    public AbstractEntity() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof AbstractEntity that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "AbstractEntity{" + "name=" + this.name + "}";
	    }
	}

	public static class ConcreteEntity extends AbstractEntity {
		private String extra;
	
	    public ConcreteEntity() { }

	    public String getExtra() { return this.extra; }

	    public void setExtra(String extra) { this.extra = extra; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof ConcreteEntity that)) return false;
	        return java.util.Objects.equals(this.extra, that.extra);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.extra);
	    }

	    @Override
	    public String toString() {
	        return "ConcreteEntity{" + "extra=" + this.extra + "}";
	    }
	}

	public static class HierarchicalDto {
		@FieldMappingRule(source = AbstractEntity.class, sourceFieldAddress = "name")
		private String label;
	
	    public HierarchicalDto() { }

	    public String getLabel() { return this.label; }

	    public void setLabel(String label) { this.label = label; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof HierarchicalDto that)) return false;
	        return java.util.Objects.equals(this.label, that.label);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.label);
	    }

	    @Override
	    public String toString() {
	        return "HierarchicalDto{" + "label=" + this.label + "}";
	    }
	}

	public static class Base {
		protected String value;
	
	    public Base() { }

	    public String getValue() { return this.value; }

	    public void setValue(String value) { this.value = value; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof Base that)) return false;
	        return java.util.Objects.equals(this.value, that.value);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.value);
	    }

	    @Override
	    public String toString() {
	        return "Base{" + "value=" + this.value + "}";
	    }
	}

	public static class Middle extends Base {
	
	    public Middle() { }
	}

	public static class Leaf extends Middle {
	
	    public Leaf() { }
	}

	public static class MostSpecificDto {
		@FieldMappingRule(source = Base.class, sourceFieldAddress = "value", fromSourceMethod = "fromBase")
		@FieldMappingRule(source = Middle.class, sourceFieldAddress = "value", fromSourceMethod = "fromMiddle")
		private String label;

		@SuppressWarnings("unused")
		private String fromBase(String v) {
			return "base:" + v;
		}

		@SuppressWarnings("unused")
		private String fromMiddle(String v) {
			return "middle:" + v;
		}
	
	    public MostSpecificDto() { }

	    public String getLabel() { return this.label; }

	    public void setLabel(String label) { this.label = label; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof MostSpecificDto that)) return false;
	        return java.util.Objects.equals(this.label, that.label);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.label);
	    }

	    @Override
	    public String toString() {
	        return "MostSpecificDto{" + "label=" + this.label + "}";
	    }
	}

	public interface InterfaceA { }
	public interface InterfaceB { }

	public static class AmbiguousEntity implements InterfaceA, InterfaceB {
		private String name;
	
	    public AmbiguousEntity() { }

	    public String getName() { return this.name; }

	    public void setName(String name) { this.name = name; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof AmbiguousEntity that)) return false;
	        return java.util.Objects.equals(this.name, that.name);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.name);
	    }

	    @Override
	    public String toString() {
	        return "AmbiguousEntity{" + "name=" + this.name + "}";
	    }
	}

	public static class AmbiguousDto {
		@FieldMappingRule(source = InterfaceA.class, sourceFieldAddress = "name")
		@FieldMappingRule(source = InterfaceB.class, sourceFieldAddress = "name")
		private String label;
	
	    public AmbiguousDto() { }

	    public String getLabel() { return this.label; }

	    public void setLabel(String label) { this.label = label; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof AmbiguousDto that)) return false;
	        return java.util.Objects.equals(this.label, that.label);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.label);
	    }

	    @Override
	    public String toString() {
	        return "AmbiguousDto{" + "label=" + this.label + "}";
	    }
	}

	public static class DuplicateExactDto {
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "firstName")
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "email")
		private String label;
	
	    public DuplicateExactDto() { }

	    public String getLabel() { return this.label; }

	    public void setLabel(String label) { this.label = label; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof DuplicateExactDto that)) return false;
	        return java.util.Objects.equals(this.label, that.label);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.label);
	    }

	    @Override
	    public String toString() {
	        return "DuplicateExactDto{" + "label=" + this.label + "}";
	    }
	}

	public static class TypedOnlyDto {
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "firstName")
		private String label;
	
	    public TypedOnlyDto() { }

	    public String getLabel() { return this.label; }

	    public void setLabel(String label) { this.label = label; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof TypedOnlyDto that)) return false;
	        return java.util.Objects.equals(this.label, that.label);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.label);
	    }

	    @Override
	    public String toString() {
	        return "TypedOnlyDto{" + "label=" + this.label + "}";
	    }
	}

	@ObjectMappingRule(source = EntityA.class,
			fromSourceMethod = "fromEntityA",
			toSourceMethod = "toEntityA")
	@ObjectMappingRule(source = EntityB.class,
			fromSourceMethod = "fromEntityB",
			toSourceMethod = "toEntityB")
	public static class ObjectMultiSourceDto {
		private String label;

		@SuppressWarnings("unused")
		private void fromEntityA(EntityA src) {
			this.label = "A:" + src.getFirstName();
		}

		@SuppressWarnings("unused")
		private void toEntityA(EntityA src) {
		}

		@SuppressWarnings("unused")
		private void fromEntityB(EntityB src) {
			this.label = "B:" + src.getPrenom();
		}

		@SuppressWarnings("unused")
		private void toEntityB(EntityB src) {
		}
	
	    public ObjectMultiSourceDto() { }

	    public String getLabel() { return this.label; }

	    public void setLabel(String label) { this.label = label; }

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof ObjectMultiSourceDto that)) return false;
	        return java.util.Objects.equals(this.label, that.label);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(this.label);
	    }

	    @Override
	    public String toString() {
	        return "ObjectMultiSourceDto{" + "label=" + this.label + "}";
	    }
	}

	// --- Tests ------------------------------------------------------------

	@Test
	public void typedRules_distinctSources_mapDifferentFields() throws MapperException {
		Mapper mapper = new Mapper(reflection)
				.configure(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, false);

		EntityA a = new EntityA();
		a.setFirstName("Jean");
		a.setEmail("jean@example.com");
		MultiSourceDto fromA = mapper.map(a, reflection.getClass(MultiSourceDto.class));
		assertEquals("Jean", fromA.getName());
		assertEquals("jean@example.com", fromA.getContact());

		EntityB b = new EntityB();
		b.setPrenom("Pierre");
		b.setCourriel("pierre@example.com");
		MultiSourceDto fromB = mapper.map(b, reflection.getClass(MultiSourceDto.class));
		assertEquals("Pierre", fromB.getName());
		assertEquals("pierre@example.com", fromB.getContact());
	}

	@Test
	public void wildcardPlusTyped_typedWinsForItsSource_wildcardForOthers() throws MapperException {
		Mapper mapper = new Mapper(reflection)
				.configure(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, false);

		EntityA a = new EntityA();
		a.setFirstName("Jean");
		WildcardOverrideDto fromA = mapper.map(a, reflection.getClass(WildcardOverrideDto.class));
		assertEquals("Jean", fromA.getName());

		EntityB b = new EntityB();
		b.setPrenom("Pierre");
		WildcardOverrideDto fromB = mapper.map(b, reflection.getClass(WildcardOverrideDto.class));
		assertEquals("Pierre", fromB.getName());
	}

	@Test
	public void hierarchicalMatch_typedOnSupertype_matchesSubtype() throws MapperException {
		Mapper mapper = new Mapper(reflection)
				.configure(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, false);

		ConcreteEntity c = new ConcreteEntity();
		c.setName("hello");
		HierarchicalDto dto = mapper.map(c, reflection.getClass(HierarchicalDto.class));
		assertEquals("hello", dto.getLabel());
	}

	@Test
	public void mostSpecific_winsAmongAssignable() throws MapperException {
		MappingRules rules = new MappingRules(reflection);
		List<MappingRule> parsed = rules.parse(
				reflection.getClass(MostSpecificDto.class),
				reflection.getClass(Leaf.class));

		assertEquals(1, parsed.size());
		MappingRule rule = parsed.get(0);
		assertNotNull(rule.fromSourceMethodAddress());
		// Middle is more specific than Base for Leaf -> Middle wins.
		assertEquals("fromMiddle", rule.fromSourceMethodAddress().toString());
	}

	@Test
	public void ambiguousIncomparableMatches_throws() {
		MappingRules rules = new MappingRules(reflection);
		assertThrows(MapperException.class,
				() -> rules.parse(reflection.getClass(AmbiguousDto.class),
						reflection.getClass(AmbiguousEntity.class)));
	}

	@Test
	public void duplicateExactSource_throws() {
		MappingRules rules = new MappingRules(reflection);
		assertThrows(MapperException.class,
				() -> rules.parse(reflection.getClass(DuplicateExactDto.class),
						reflection.getClass(EntityA.class)));
	}

	@Test
	public void typedOnly_nonMatchingSource_yieldsNoRule() throws MapperException {
		MappingRules rules = new MappingRules(reflection);
		List<MappingRule> parsed = rules.parse(
				reflection.getClass(TypedOnlyDto.class),
				reflection.getClass(EntityB.class));
		// Typed rule does not match EntityB and there is no wildcard → no rule emitted.
		assertEquals(0, parsed.size());
	}

	@Test
	public void objectMappingRule_perSource_picksRightRule() throws MapperException {
		MappingRules rules = new MappingRules(reflection);

		List<MappingRule> parsedA = rules.parse(
				reflection.getClass(ObjectMultiSourceDto.class),
				reflection.getClass(EntityA.class));
		assertEquals(1, parsedA.size());
		assertEquals("fromEntityA", parsedA.get(0).fromSourceMethodAddress().toString());

		List<MappingRule> parsedB = rules.parse(
				reflection.getClass(ObjectMultiSourceDto.class),
				reflection.getClass(EntityB.class));
		assertEquals(1, parsedB.size());
		assertEquals("fromEntityB", parsedB.get(0).fromSourceMethodAddress().toString());
	}

	@Test
	public void cacheIsolation_sameDtoFromTwoSources_storesTwoEntries() throws MapperException {
		Mapper mapper = new Mapper(reflection)
				.configure(MapperConfigurationItem.AUTO_CONVENTION_MAPPING, false);

		EntityA a = new EntityA();
		a.setFirstName("Jean");
		a.setEmail("jean@example.com");
		mapper.map(a, reflection.getClass(MultiSourceDto.class));

		EntityB b = new EntityB();
		b.setPrenom("Pierre");
		b.setCourriel("pierre@example.com");
		mapper.map(b, reflection.getClass(MultiSourceDto.class));

		assertEquals(2, mapper.mappingConfigurations.size());
	}
}
