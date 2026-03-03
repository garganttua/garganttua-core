package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.ObjectMappingRule;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

import lombok.Getter;
import lombok.Setter;

class OtherGenericEntity extends GenericEntity {
	@Getter
	@Setter
	long longField;
}

class GenericEntityWithObjectMapping extends GenericEntity {
	@Getter
	@Setter
	long longField;
}

class GenericEntityFromTwoDtos extends GenericEntity {
	@Getter
	@Setter
	long longField;

	@Getter
	@Setter
	long otherField;
}

class OtherGenericDto extends GenericDto {

	@FieldMappingRule(sourceFieldAddress = "longField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
	String longField;

	public OtherGenericDto() {
	}

	@SuppressWarnings("unused")
	private String fromMethod(long longField) {
		return String.valueOf(longField);
	}

	@SuppressWarnings("unused")
	private long toMethod(String value) {
		return Long.valueOf(value);
	}
}

class SecondGenericDto extends GenericDto {

	@FieldMappingRule(sourceFieldAddress = "otherField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
	String longField;

	public SecondGenericDto() {
	}

	@SuppressWarnings("unused")
	private String fromMethod(long longField) {
		return String.valueOf(longField);
	}

	@SuppressWarnings("unused")
	private long toMethod(String value) {
		return Long.valueOf(value);
	}
}

@ObjectMappingRule(fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
class GenericDtoWithObjectMapping extends GenericDto {

	@FieldMappingRule(sourceFieldAddress = "longField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
	String longField;

	@SuppressWarnings("unused")
	private void fromMethod(GenericEntityWithObjectMapping entity) {
		this.id = entity.getId();
		this.uuid = entity.getUuid();
		this.longField = String.valueOf(entity.getLongField());
	}

	@SuppressWarnings("unused")
	private void toMethod(GenericEntityWithObjectMapping entity) {

	}
}

public class MapperTest {

	private static IReflection reflection;

	@BeforeAll
	static void setUpReflection() throws Exception {
		reflection = ReflectionBuilder.builder()
				.withProvider(new RuntimeReflectionProvider())
				.build();
		IClass.setReflection(reflection);
	}

	@AfterAll
	static void tearDownReflection() {
		IClass.setReflection(null);
	}

	@Test
	public void testRegularFieldMapping() throws MapperException {

		GenericEntity entity = new GenericEntity();
		entity.setUuid("uuid");
		entity.setId("id");

		GenericDto dest = new Mapper(reflection).configure(MapperConfigurationItem.FAIL_ON_ERROR, false).map(entity,
				reflection.getClass(GenericDto.class));

		assertNotNull(dest);
		assertEquals("uuid", dest.getUuid());
		assertEquals("id", dest.getId());

	}

	@Test
	public void testReverseFieldMapping() throws MapperException {

		GenericDto dto = new GenericDto();
		dto.setUuid("uuid");
		dto.setId("id");

		GenericEntity dest = new Mapper(reflection).configure(MapperConfigurationItem.FAIL_ON_ERROR, false).map(dto,
				reflection.getClass(GenericEntity.class));

		assertNotNull(dest);
		assertEquals("uuid", dest.getUuid());
		assertEquals("id", dest.getId());

	}

	@Test
	public void testMappingConfigurationNotFailOnError() {
		GenericEntity entity = new GenericEntity();
		entity.setUuid("uuid");
		entity.setId("id");

		assertThrows(MapperException.class, () -> {
			new Mapper(reflection).configure(MapperConfigurationItem.FAIL_ON_ERROR, true).map(entity, reflection.getClass(GenericDto.class));
		});
	}

	@Test
	public void testRecordMappingConfiguration() throws MapperException {
		Mapper mapper = new Mapper(reflection).configure(MapperConfigurationItem.FAIL_ON_ERROR, false);
		mapper.recordMappingConfiguration(reflection.getClass(GenericEntity.class), reflection.getClass(GenericDto.class));

		assertEquals(1, mapper.mappingConfigurations.size());
	}

	@Test
	public void testConcurrentMapping() throws Exception {
		Mapper mapper = new Mapper(reflection).configure(MapperConfigurationItem.FAIL_ON_ERROR, false);
		int threadCount = 100;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		GenericEntity entity = new GenericEntity();
		entity.setUuid("uuid");
		entity.setId("id");


		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			futures.add(executor.submit(() -> {
				try {
					mapper.map(entity, reflection.getClass(GenericDto.class));
				} catch (MapperException e) {
					fail("Mapping failed: " + e.getMessage());
				}
			}));
		}

		for (Future<?> future : futures) {
			future.get();
		}

		executor.shutdown();
		assertEquals(1, mapper.mappingConfigurations.size());
	}
}
