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

import lombok.Data;

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

	@Data
	public static class EntityA {
		private String firstName;
		private String email;
	}

	@Data
	public static class EntityB {
		private String prenom;
		private String courriel;
	}

	@Data
	public static class MultiSourceDto {
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "firstName")
		@FieldMappingRule(source = EntityB.class, sourceFieldAddress = "prenom")
		private String name;

		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "email")
		@FieldMappingRule(source = EntityB.class, sourceFieldAddress = "courriel")
		private String contact;
	}

	@Data
	public static class WildcardOverrideDto {
		@FieldMappingRule(sourceFieldAddress = "firstName")
		@FieldMappingRule(source = EntityB.class, sourceFieldAddress = "prenom")
		private String name;
	}

	@Data
	public static class AbstractEntity {
		protected String name;
	}

	@Data
	public static class ConcreteEntity extends AbstractEntity {
		private String extra;
	}

	@Data
	public static class HierarchicalDto {
		@FieldMappingRule(source = AbstractEntity.class, sourceFieldAddress = "name")
		private String label;
	}

	@Data
	public static class Base {
		protected String value;
	}

	@Data
	public static class Middle extends Base {
	}

	@Data
	public static class Leaf extends Middle {
	}

	@Data
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
	}

	public interface InterfaceA { }
	public interface InterfaceB { }

	@Data
	public static class AmbiguousEntity implements InterfaceA, InterfaceB {
		private String name;
	}

	@Data
	public static class AmbiguousDto {
		@FieldMappingRule(source = InterfaceA.class, sourceFieldAddress = "name")
		@FieldMappingRule(source = InterfaceB.class, sourceFieldAddress = "name")
		private String label;
	}

	@Data
	public static class DuplicateExactDto {
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "firstName")
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "email")
		private String label;
	}

	@Data
	public static class TypedOnlyDto {
		@FieldMappingRule(source = EntityA.class, sourceFieldAddress = "firstName")
		private String label;
	}

	@ObjectMappingRule(source = EntityA.class,
			fromSourceMethod = "fromEntityA",
			toSourceMethod = "toEntityA")
	@ObjectMappingRule(source = EntityB.class,
			fromSourceMethod = "fromEntityB",
			toSourceMethod = "toEntityB")
	@Data
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
