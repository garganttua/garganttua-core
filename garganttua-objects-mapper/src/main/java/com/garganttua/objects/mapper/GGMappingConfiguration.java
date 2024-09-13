package com.garganttua.objects.mapper;

import java.util.List;
import java.util.Objects;

import com.garganttua.objects.mapper.rules.GGMappingRule;

public record GGMappingConfiguration (
		Class<?> source, 
		Class<?> destination, 
		List<GGMappingRule> sourceRules, 
		List<GGMappingRule> destinationRules, 
		GGMappingDirection mappingDirection) {

	 @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;
	        GGMappingConfiguration that = (GGMappingConfiguration) o;
	        return Objects.equals(source, that.source) &&
	                Objects.equals(destination, that.destination) /*&&
	                Objects.equals(sourceRules, that.sourceRules) &&
	                Objects.equals(destinationRules, that.destinationRules) &&
	                mappingDirection == that.mappingDirection*/;
	    }

	    // hashCode method
	    @Override
	    public int hashCode() {
	        return Objects.hash(source, destination, sourceRules, destinationRules, mappingDirection);
	    }

	    // toString method
	    @Override
	    public String toString() {
	        return "GGMappingConfiguration{" +
	                "source=" + source +
	                ", destination=" + destination +
	                ", sourceRules=" + sourceRules +
	                ", destinationRules=" + destinationRules +
	                ", mappingDirection=" + mappingDirection +
	                '}';
	    }
}
