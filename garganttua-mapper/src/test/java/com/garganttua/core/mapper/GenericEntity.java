package com.garganttua.core.mapper;

public class GenericEntity {
	
	protected String uuid;
	
	protected String id;
	

    public GenericEntity() { }

    public String getUuid() { return this.uuid; }

    public String getId() { return this.id; }

    public void setUuid(String uuid) { this.uuid = uuid; }

    public void setId(String id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericEntity that)) return false;
        return java.util.Objects.equals(this.uuid, that.uuid) && java.util.Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.uuid, this.id);
    }

    @Override
    public String toString() {
        return "GenericEntity{" + "uuid=" + this.uuid + ", " + "id=" + this.id + "}";
    }
}