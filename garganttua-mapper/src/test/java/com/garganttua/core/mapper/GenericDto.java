/*******************************************************************************
 * Copyright (c) 2022 Jérémy COLOMBET
 *******************************************************************************/
package com.garganttua.core.mapper;

import com.garganttua.core.mapper.annotations.FieldMappingRule;

/**
 * 
 * @author J.Colombet
 *
 * @param <Entity>
 */
public class GenericDto {
	
	@FieldMappingRule(sourceFieldAddress = "uuid")
	protected String uuid;
	
	@FieldMappingRule(sourceFieldAddress = "id")
	protected String id;
	
	@FieldMappingRule(sourceFieldAddress = "tenantId")
	protected String tenantId;

    public GenericDto() { }

    public String getUuid() { return this.uuid; }

    public String getId() { return this.id; }

    public String getTenantId() { return this.tenantId; }

    public void setUuid(String uuid) { this.uuid = uuid; }

    public void setId(String id) { this.id = id; }

    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericDto that)) return false;
        return java.util.Objects.equals(this.uuid, that.uuid) && java.util.Objects.equals(this.id, that.id) && java.util.Objects.equals(this.tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.uuid, this.id, this.tenantId);
    }

    @Override
    public String toString() {
        return "GenericDto{" + "uuid=" + this.uuid + ", " + "id=" + this.id + ", " + "tenantId=" + this.tenantId + "}";
    }
}
