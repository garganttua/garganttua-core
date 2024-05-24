package com.garganttua.objects.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.garganttua.objects.mapper.annotations.GGFieldMappingRule;
import com.garganttua.objects.mapper.annotations.GGObjectMappingRule;

import lombok.Getter;
import lombok.Setter;


class GenericEntity extends GenericGGEntity {
	@Getter
	@Setter
	long longField;
}


class GenericEntityWithObjectMapping extends GenericGGEntity {
	@Getter
	@Setter
	long longField;
}

class GenericEntityFromTwoDtos extends GenericGGEntity {
	@Getter
	@Setter
	long longField;
	
	@Getter
	@Setter
	long otherField;
}

class GenericDto extends GenericGGDto {
	
	@GGFieldMappingRule(sourceFieldAddress = "longField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
	String longField;
	
	public GenericDto() {
	}
	
	private String fromMethod(long longField) {
		return String.valueOf(longField);
	}
	
	private long toMethod(String value) {
		return Long.valueOf(value);
	}
}

class SecondGenericDto extends GenericGGDto {
	
	@GGFieldMappingRule(sourceFieldAddress = "otherField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
	String longField;
	
	public SecondGenericDto() {
	}
	
	private String fromMethod(long longField) {
		return String.valueOf(longField);
	}
	
	private long toMethod(String value) {
		return Long.valueOf(value);
	}
}

@GGObjectMappingRule(fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
class GenericDtoWithObjectMapping extends GenericGGDto {
	
	@GGFieldMappingRule(sourceFieldAddress = "longField", fromSourceMethod = "fromMethod", toSourceMethod = "toMethod")
	String longField;
	
	
	private void fromMethod(GenericEntityWithObjectMapping entity) {
		this.id = entity.getId();
		this.uuid = entity.getUuid();
		this.longField = String.valueOf(entity.getLongField());
	}
	
	private void toMethod(GenericEntityWithObjectMapping entity) {

	}
}

public class GGMapperTest {

	@Test
	public void testRegularFieldMapping() throws GGMapperException {
		
		GenericGGEntity entity = new GenericGGEntity();
		entity.setUuid("uuid");
		entity.setId("id");
		
		GenericGGDto dest = new GGMapper().configure(GGMapperConfigurationItem.FAIL_ON_ERROR, false).map(entity, GenericGGDto.class);
		
		assertNotNull(dest);
		assertEquals("uuid", dest.getUuid());
		assertEquals("id", dest.getId());
		
	}
	
	@Test
	public void testReverseFieldMapping() throws GGMapperException {
		
		GenericGGDto dto = new GenericGGDto();
		dto.setUuid("uuid");
		dto.setId("id");
		
		GenericGGEntity dest = new GGMapper().configure(GGMapperConfigurationItem.FAIL_ON_ERROR, false).map(dto, GenericGGEntity.class);
		
		assertNotNull(dest);
		assertEquals("uuid", dest.getUuid());
		assertEquals("id", dest.getId());
		
	}
	
	@Test
	public void testMappingConfigurationNotFailOnError() {
		GenericGGEntity entity = new GenericGGEntity();
		entity.setUuid("uuid");
		entity.setId("id");
		
		assertThrows(GGMapperException.class, () -> {
			new GGMapper().configure(GGMapperConfigurationItem.FAIL_ON_ERROR, true).map(entity, GenericGGDto.class);
		});
	}
}
