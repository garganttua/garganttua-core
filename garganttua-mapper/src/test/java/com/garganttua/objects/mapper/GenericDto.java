/*******************************************************************************
 * Copyright (c) 2022 Jérémy COLOMBET
 *******************************************************************************/
package com.garganttua.objects.mapper;

import com.garganttua.core.mapper.annotations.FieldMappingRule;

import lombok.Data;

/**
 * 
 * @author J.Colombet
 *
 * @param <Entity>
 */
@Data
public class GenericDto {
	
	@FieldMappingRule(sourceFieldAddress = "uuid")
	protected String uuid;
	
	@FieldMappingRule(sourceFieldAddress = "id")
	protected String id;
	
	@FieldMappingRule(sourceFieldAddress = "tenantId")
	protected String tenantId;

}
