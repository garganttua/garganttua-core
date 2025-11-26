package com.garganttua.core.mapper;

import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record MappingConfiguration(
		Class<?> source,
		Class<?> destination,
		List<MappingRule> sourceRules,
		List<MappingRule> destinationRules,
		MappingDirection mappingDirection) {

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MappingConfiguration that = (MappingConfiguration) o;
		return Objects.equals(source, that.source) &&
				Objects.equals(destination, that.destination) /*
																 * &&
																 * Objects.equals(sourceRules, that.sourceRules) &&
																 * Objects.equals(destinationRules,
																 * that.destinationRules) &&
																 * mappingDirection == that.mappingDirection
																 */;
	}

	// hashCode method
	@Override
	public int hashCode() {
		return Objects.hash(source, destination, sourceRules, destinationRules, mappingDirection);
	}

	// toString method
	@Override
	public String toString() {
		return "MappingConfiguration{" +
				"source=" + source +
				", destination=" + destination +
				", sourceRules=" + sourceRules +
				", destinationRules=" + destinationRules +
				", mappingDirection=" + mappingDirection +
				'}';
	}
}
