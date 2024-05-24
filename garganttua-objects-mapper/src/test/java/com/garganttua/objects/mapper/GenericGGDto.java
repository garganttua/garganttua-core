/*******************************************************************************
 * Copyright (c) 2022 Jérémy COLOMBET
 *******************************************************************************/
package com.garganttua.objects.mapper;

import com.garganttua.objects.mapper.annotations.GGFieldMappingRule;

import lombok.Data;

/**
 * 
 * @author J.Colombet
 *
 * @param <Entity>
 */
@Data
public class GenericGGDto {
	
	@GGFieldMappingRule(sourceFieldAddress = "uuid")
	protected String uuid;
	
	@GGFieldMappingRule(sourceFieldAddress = "id")
	protected String id;
	
	@GGFieldMappingRule(sourceFieldAddress = "tenantId")
	protected String tenantId;

}
