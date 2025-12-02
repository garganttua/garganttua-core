package com.garganttua.core.mapper;

import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

/**
 * Represents the complete mapping configuration between two classes.
 * <p>
 * A mapping configuration encapsulates all the rules and metadata needed to map
 * objects from a source class to a destination class. It supports bidirectional
 * mapping with separate rules for each direction.
 * </p>
 *
 * <h2>Components:</h2>
 * <ul>
 *   <li><b>source</b>: The source class type</li>
 *   <li><b>destination</b>: The destination class type</li>
 *   <li><b>sourceRules</b>: Mapping rules applied when mapping from source to destination</li>
 *   <li><b>destinationRules</b>: Mapping rules applied when mapping from destination to source (reverse mapping)</li>
 *   <li><b>mappingDirection</b>: Indicates whether this is a regular or reverse mapping</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Define mapping rules
 * List&lt;MappingRule&gt; rules = List.of(
 *     new MappingRule(
 *         new ObjectAddress("user.firstName"),
 *         new ObjectAddress("dto.name"),
 *         String.class,
 *         null,
 *         null
 *     ),
 *     new MappingRule(
 *         new ObjectAddress("user.email"),
 *         new ObjectAddress("dto.emailAddress"),
 *         String.class,
 *         null,
 *         null
 *     )
 * );
 *
 * // Create mapping configuration
 * MappingConfiguration config = new MappingConfiguration(
 *     User.class,
 *     UserDTO.class,
 *     rules,
 *     List.of(), // No reverse rules
 *     MappingDirection.REGULAR
 * );
 *
 * // Use with mapper
 * IMapper mapper = new Mapper();
 * UserDTO dto = mapper.map(user, UserDTO.class);
 * </pre>
 *
 * @param source the source class
 * @param destination the destination class
 * @param sourceRules the list of mapping rules for source to destination
 * @param destinationRules the list of mapping rules for destination to source
 * @param mappingDirection the direction of mapping
 *
 * @since 2.0.0-ALPHA01
 */
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
